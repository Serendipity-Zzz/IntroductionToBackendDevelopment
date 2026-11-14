package cn.meeting.signup;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static cn.meeting.signup.Models.*;

@Service
public class SignupService {
    private final SignupRepository repository;
    public SignupService(SignupRepository repository){this.repository=repository;}
    public EventView event(long eventId,long userId){
        positive(eventId);EventRow event=repository.findEvent(eventId).orElseThrow(this::missingEvent);
        int count=repository.count(eventId);
        Long own=repository.findForEventAndUser(eventId,userId).map(RegistrationView::id).orElse(null);
        return new EventView(event.id(),event.title(),event.capacity(),count,Math.max(0,event.capacity()-count),own);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public RegistrationView register(long eventId,long userId){
        positive(eventId);EventRow event=repository.lockEvent(eventId).orElseThrow(this::missingEvent);
        if(repository.findForEventAndUser(eventId,userId).isPresent())throw new ApiException(409,"ALREADY_REGISTERED","你已经报名了这场活动");
        if(repository.count(eventId)>=event.capacity())throw new ApiException(409,"EVENT_FULL","名额已满，请关注下一场活动");
        long id=repository.insert(eventId,userId);
        return repository.findOwned(id,userId).orElseThrow();
    }
    public RegistrationView registration(long id,long userId){
        positive(id);return repository.findOwned(id,userId).orElseThrow(this::missingRegistration);
    }
    public RegistrationView myRegistration(long eventId,long userId){
        positive(eventId);repository.findEvent(eventId).orElseThrow(this::missingEvent);
        return repository.findForEventAndUser(eventId,userId).orElseThrow(this::missingRegistration);
    }
    public List<RegistrationView> mine(long userId){return repository.findMine(userId);}
    private void positive(long id){if(id<=0)throw new ApiException(400,"VALIDATION_ERROR","ID必须是正整数");}
    private ApiException missingEvent(){return new ApiException(404,"EVENT_NOT_FOUND","活动不存在");}
    private ApiException missingRegistration(){return new ApiException(404,"REGISTRATION_NOT_FOUND","没有找到这条报名");}
}
