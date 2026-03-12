package org.example.greenloginbe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogResponse {
    private Integer id;
    private Integer orderId;
    private String actionType;
    private Map<String, Object> oldData;
    private Map<String, Object> newData;
    private Instant createdAt;
    
    // User info
    private Integer userId;
    private String username;
    private String userFullName;
}
