package com.upself.biz.learning.mapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface DailyCheckInMapper {
 List<LocalDate> dates(@Param("user") Long user);
 int insert(@Param("user") Long user,@Param("date") LocalDate date);
}
