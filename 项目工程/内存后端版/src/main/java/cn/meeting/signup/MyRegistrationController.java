package cn.meeting.signup;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import static cn.meeting.signup.Models.*;

@RestController
public class MyRegistrationController {
    private final AuthService auth;private final SignupService service;
    public MyRegistrationController(AuthService auth,SignupService service){this.auth=auth;this.service=service;}
    @GetMapping("/api/events/{eventId}/my-registration")
    public RegistrationView myRegistration(@PathVariable long eventId,HttpServletRequest request){
        long userId=auth.require(request).id();
        return service.myRegistration(eventId,userId);
    }
}
