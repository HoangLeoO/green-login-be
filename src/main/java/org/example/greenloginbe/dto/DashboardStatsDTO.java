package org.example.greenloginbe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    // Metrics
    private long totalOrdersToday;
    private double ordersGrowth; // % so với hôm qua
    
    private BigDecimal totalRevenueToday;
    private double revenueGrowth; // % so với hôm qua
    
    private long debtOrdersToday;
    private BigDecimal debtAmountToday;
    
    // Chart Data
    private List<DailyRevenueStats> weeklyStats;
    
    // Recent Data
    private List<RecentOrderDTO> recentOrders;
    private List<TopCustomerDTO> topCustomers;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DailyRevenueStats {
        private String name; // Thứ/Ngày
        private BigDecimal revenue;
        private long orders;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class RecentOrderDTO {
        private String id;
        private String customerName;
        private BigDecimal amount;
        private String status;
        private String time;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TopCustomerDTO {
        private String name;
        private BigDecimal totalSpent;
        private int orders;
    }
}
