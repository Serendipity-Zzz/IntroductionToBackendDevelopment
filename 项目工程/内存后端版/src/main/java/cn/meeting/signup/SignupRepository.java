package cn.meeting.signup;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static cn.meeting.signup.Models.*;

/** 教学用内存存储：不连接数据库，不写磁盘，重启重新初始化。 */
@Repository
public class SignupRepository {
    private final Map<Long,User> users=new ConcurrentHashMap<>();
    private final Map<Long,EventRow> events=Map.of(1L,new EventRow(1,"后端开发体验课",1),10L,new EventRow(10,"接口练习场",3));
    private final Map<Long,RegistrationView> registrations=new ConcurrentHashMap<>();
    private final AtomicLong nextUser=new AtomicLong(5),nextRegistration=new AtomicLong(1);
    public SignupRepository(){
        String hash=new BCryptPasswordEncoder().encode("demo123");
        users.put(1L,new User(1,"alice","林同学",hash));users.put(2L,new User(2,"bob","周同学",hash));
        users.put(3L,new User(3,"zhh","zhh",hash));users.put(4L,new User(4,"myh","myh",hash));
    }
    public Optional<User> findUser(String username){return users.values().stream().filter(u->u.username().equals(username)).findFirst();}
    public Optional<User> findUser(long id){return Optional.ofNullable(users.get(id));}
    public synchronized long createUser(String username,String displayName,String hash){
        if(findUser(username).isPresent())throw new ApiException(409,"USERNAME_TAKEN","这个账号已有人使用，请换一个账号");
        long id=nextUser.getAndIncrement();users.put(id,new User(id,username,displayName,hash));return id;
    }
    public Optional<EventRow> findEvent(long id){return Optional.ofNullable(events.get(id));}
    public Optional<EventRow> lockEvent(long id){return findEvent(id);}
    public int count(long id){return (int)registrations.values().stream().filter(r->r.eventId()==id).count();}
    public Optional<RegistrationView> findOwned(long id,long userId){return Optional.ofNullable(registrations.get(id)).filter(r->r.userId()==userId);}
    public Optional<RegistrationView> findForEventAndUser(long eventId,long userId){return registrations.values().stream().filter(r->r.eventId()==eventId&&r.userId()==userId).findFirst();}
    public List<RegistrationView> findMine(long userId){return registrations.values().stream().filter(r->r.userId()==userId).sorted(Comparator.comparingLong(RegistrationView::id).reversed()).toList();}
    public long insert(long eventId,long userId){
        long id=nextRegistration.getAndIncrement();User u=users.get(userId);EventRow e=events.get(eventId);
        registrations.put(id,new RegistrationView(id,eventId,e.title(),userId,u.username(),u.displayName(),LocalDateTime.now()));return id;
    }
}
