package cn.meeting.signup;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import static cn.meeting.signup.Models.*;

@Repository
public class SignupRepository {
    private final JdbcTemplate jdbc;
    public SignupRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

    // 课堂填写位置1：?对应本次请求传入的活动ID。
    private static final String FIND_EVENT_SQL = "SELECT id, title, capacity FROM events WHERE id = ?";
    // 课堂填写位置2：两个?依次对应活动ID和已登录用户ID。
    private static final String INSERT_REGISTRATION_SQL = "INSERT INTO registrations(event_id, user_id) VALUES (?, ?)";

    public Optional<User> findUser(String username) {
        return jdbc.query("SELECT id,username,display_name,password_hash FROM users WHERE username=?",
                (rs,n)->new User(rs.getLong("id"),rs.getString("username"),rs.getString("display_name"),rs.getString("password_hash")),username).stream().findFirst();
    }
    public Optional<User> findUser(long id) {
        return jdbc.query("SELECT id,username,display_name,password_hash FROM users WHERE id=?",
                (rs,n)->new User(rs.getLong("id"),rs.getString("username"),rs.getString("display_name"),rs.getString("password_hash")),id).stream().findFirst();
    }
    public long createUser(String username,String displayName,String passwordHash) {
        var keys=new GeneratedKeyHolder();
        jdbc.update(connection->{
            var statement=connection.prepareStatement(
                    "INSERT INTO users(username,display_name,password_hash) VALUES (?,?,?)",Statement.RETURN_GENERATED_KEYS);
            statement.setString(1,username);statement.setString(2,displayName);statement.setString(3,passwordHash);
            return statement;
        },keys);
        Number key=keys.getKey();
        if(key==null)throw new IllegalStateException("没有取得账号ID");
        return key.longValue();
    }
    public Optional<EventRow> findEvent(long id) {
        requireSql(FIND_EVENT_SQL,"请先填写FIND_EVENT_SQL，再重新构建并启动服务");
        return jdbc.query(FIND_EVENT_SQL,(rs,n)->new EventRow(rs.getLong("id"),rs.getString("title"),rs.getInt("capacity")),id).stream().findFirst();
    }
    public Optional<EventRow> lockEvent(long id) {
        return jdbc.query("SELECT id,title,capacity FROM events WHERE id=? FOR UPDATE",
                (rs,n)->new EventRow(rs.getLong("id"),rs.getString("title"),rs.getInt("capacity")),id).stream().findFirst();
    }
    public int count(long eventId) {
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM registrations WHERE event_id=?",Integer.class,eventId);
        return count==null?0:count;
    }
    private static final String REG_QUERY="""
        SELECT r.id,r.event_id,e.title AS event_title,r.user_id,u.username,u.display_name,r.created_at
        FROM registrations r JOIN events e ON e.id=r.event_id JOIN users u ON u.id=r.user_id
        """;
    private org.springframework.jdbc.core.RowMapper<RegistrationView> mapper(){
        return (rs,n)->new RegistrationView(rs.getLong("id"),rs.getLong("event_id"),rs.getString("event_title"),
                rs.getLong("user_id"),rs.getString("username"),rs.getString("display_name"),rs.getTimestamp("created_at").toLocalDateTime());
    }
    public Optional<RegistrationView> findOwned(long id,long userId){
        return jdbc.query(REG_QUERY+" WHERE r.id=? AND r.user_id=?",mapper(),id,userId).stream().findFirst();
    }
    public Optional<RegistrationView> findForEventAndUser(long eventId,long userId){
        return jdbc.query(REG_QUERY+" WHERE r.event_id=? AND r.user_id=?",mapper(),eventId,userId).stream().findFirst();
    }
    public List<RegistrationView> findMine(long userId){
        return jdbc.query(REG_QUERY+" WHERE r.user_id=? ORDER BY r.id DESC",mapper(),userId);
    }
    public long insert(long eventId,long userId){
        requireSql(INSERT_REGISTRATION_SQL,"请先填写INSERT_REGISTRATION_SQL，再重新构建并启动服务");
        var keys=new GeneratedKeyHolder();
        jdbc.update(c->{var ps=c.prepareStatement(INSERT_REGISTRATION_SQL,Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1,eventId);ps.setLong(2,userId);return ps;},keys);
        Number key=keys.getKey();if(key==null)throw new IllegalStateException("没有取得报名ID");
        return key.longValue();
    }
    private void requireSql(String sql,String hint){
        if(sql.isBlank())throw new ApiException(501,"LAB_SQL_REQUIRED",hint);
    }
}
