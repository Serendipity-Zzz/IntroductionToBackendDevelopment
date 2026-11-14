package cn.meeting.signup;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import static cn.meeting.signup.Models.*;

@Service
public class AuthService {
    private final SignupRepository repository;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    private record Session(long userId,Instant expiresAt){}
    private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>();
    public AuthService(SignupRepository repository){this.repository=repository;}
    public PublicUser createAccount(CreateAccountRequest request){
        if(request.password().getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(400,"VALIDATION_ERROR","密码的UTF-8长度不能超过72字节，请缩短密码");
        if(repository.findUser(request.username()).isPresent())
            throw new ApiException(409,"USERNAME_TAKEN","这个账号已有人使用，请换一个账号");
        long id=repository.createUser(request.username(),request.displayName(),encoder.encode(request.password()));
        return new PublicUser(id,request.username(),request.displayName());
    }

    public LoginResult login(LoginRequest request){
        if(request.password().getBytes(StandardCharsets.UTF_8).length>72)throw unauthenticated("账号或密码不正确");
        User user=repository.findUser(request.username()).orElseThrow(()->unauthenticated("账号或密码不正确"));
        if(!encoder.matches(request.password(),user.passwordHash()))throw unauthenticated("账号或密码不正确");
        sessions.entrySet().removeIf(e->e.getValue().expiresAt().isBefore(Instant.now()));
        String token=UUID.randomUUID().toString();
        sessions.put(token,new Session(user.id(),Instant.now().plusSeconds(7200)));
        return new LoginResult(token,publicUser(user),7200);
    }
    public PublicUser require(HttpServletRequest request){
        String token=token(request);Session session=sessions.get(token);
        if(session==null||session.expiresAt().isBefore(Instant.now())){
            sessions.remove(token);throw unauthenticated("登录已失效，请重新登录");
        }
        return repository.findUser(session.userId()).map(this::publicUser)
                .orElseThrow(()->unauthenticated("账号不存在"));
    }
    public void logout(HttpServletRequest request){sessions.remove(token(request));}
    private String token(HttpServletRequest request){
        String header=request.getHeader("Authorization");
        if(header==null||!header.startsWith("Bearer ")||header.length()<=7)throw unauthenticated("请先登录");
        return header.substring(7);
    }
    private PublicUser publicUser(User user){return new PublicUser(user.id(),user.username(),user.displayName());}
    private ApiException unauthenticated(String text){return new ApiException(401,"UNAUTHENTICATED",text);}
}
