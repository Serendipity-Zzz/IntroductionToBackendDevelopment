package cn.meeting.signup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static cn.meeting.signup.Models.*;

@RestController
public class SignupController {
    private final AuthService auth;private final SignupService service;
    public SignupController(AuthService auth,SignupService service){this.auth=auth;this.service=service;}
    @GetMapping("/health") public Map<String,String> health(){return Map.of("status","ok");}
    @PostMapping("/api/auth/login") public LoginResult login(@Valid @RequestBody LoginRequest body){return auth.login(body);}
    @PostMapping("/api/auth/register") public ResponseEntity<PublicUser> createAccount(@Valid @RequestBody CreateAccountRequest body){
        return ResponseEntity.status(201).body(auth.createAccount(body));
    }
    @PostMapping("/api/auth/logout") public ResponseEntity<Void> logout(HttpServletRequest request){auth.logout(request);return ResponseEntity.noContent().build();}
    @GetMapping("/api/me") public PublicUser me(HttpServletRequest request){return auth.require(request);}
    @GetMapping("/api/events/{eventId}") public EventView event(@PathVariable long eventId,HttpServletRequest request){return service.event(eventId,auth.require(request).id());}
    @PostMapping("/api/events/{eventId}/registrations") public ResponseEntity<RegistrationView> register(
            @PathVariable long eventId,@RequestBody RegistrationRequest body,HttpServletRequest request){
        var result=service.register(eventId,auth.require(request).id());
        return ResponseEntity.created(URI.create("/api/registrations/"+result.id())).body(result);
    }
    @GetMapping("/api/registrations/{registrationId}") public RegistrationView registration(
            @PathVariable long registrationId,HttpServletRequest request){return service.registration(registrationId,auth.require(request).id());}
    @GetMapping("/api/me/registrations") public Map<String,?> mine(HttpServletRequest request){return Map.of("items",service.mine(auth.require(request).id()));}
}
