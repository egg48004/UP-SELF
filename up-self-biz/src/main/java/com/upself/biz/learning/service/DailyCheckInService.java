package com.upself.biz.learning.service;
import com.upself.biz.learning.mapper.DailyCheckInMapper;
import com.upself.common.utils.SecurityUtils;
import com.upself.common.exception.ServiceException;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
@Service
public class DailyCheckInService {
 @Autowired private DailyCheckInMapper mapper;
 private final Clock clock;
 public DailyCheckInService(){this(Clock.systemUTC());}
 public DailyCheckInService(Clock clock){this.clock=clock;}
 private Long owner(){if(!"student".equals(SecurityUtils.getLoginUser().getRoleKey()))throw new ServiceException(403,"This action is available to students only.");return SecurityUtils.getLoginUser().getUserId();}
 private LocalDate today(){return LocalDate.now(clock.withZone(ZoneId.of("Asia/Kuala_Lumpur")));}
 public Map<String,Object> read(){Long user=owner();return view(today(),mapper.dates(user));}
 public Map<String,Object> checkIn(){Long user=owner();LocalDate date=today();try{mapper.insert(user,date);}catch(DuplicateKeyException alreadyCheckedIn){/* Same student and local date: return existing state. */}return view(date,mapper.dates(user));}
 public static Map<String,Object> view(LocalDate today,List<LocalDate> dates){
 Set<LocalDate> recorded=new HashSet<>(dates);boolean checked=recorded.contains(today);
 LocalDate cursor=checked?today:today.minusDays(1);int streak=0;
 while(recorded.contains(cursor)){streak++;cursor=cursor.minusDays(1);}
 List<String> history=new ArrayList<>();for(int i=0;i<30;i++){LocalDate day=today.minusDays(i);if(recorded.contains(day))history.add(day.toString());}
 Map<String,Object> result=new LinkedHashMap<>();result.put("today",today.toString());result.put("checkedIn",checked);result.put("streak",streak);result.put("history",history);return result;
 }
}
