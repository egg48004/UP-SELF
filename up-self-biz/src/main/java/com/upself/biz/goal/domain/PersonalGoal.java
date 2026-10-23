package com.upself.biz.goal.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import lombok.Data;

@Data
public class PersonalGoal {
    private Long id;
    @JsonIgnore private Long studentId;
    private String title;
    private String details;
    private String status;
    private Date completedAt;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
