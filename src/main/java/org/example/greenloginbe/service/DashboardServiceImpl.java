package org.example.greenloginbe.service;

import lombok.RequiredArgsConstructor;
import org.example.greenloginbe.dto.DashboardStatsDTO;
import org.example.greenloginbe.entity.Order;
import org.example.greenloginbe.repository.CustomerRepository;
import org.example.greenloginbe.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate startOfWeek = today.minusDays(6);

        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 1. Metrics Today
        long ordersToday = orderRepository.countOrdersByDate(today);
        long ordersYesterday = orderRepository.countOrdersByDate(yesterday);
        stats.setTotalOrdersToday(ordersToday);
        stats.setOrdersGrowth(calculateGrowth(ordersToday, ordersYesterday));

        BigDecimal revenueToday = orderRepository.sumRevenueByDate(today) != null ? orderRepository.sumRevenueByDate(today) : BigDecimal.ZERO;
        BigDecimal revenueYesterday = orderRepository.sumRevenueByDate(yesterday) != null ? orderRepository.sumRevenueByDate(yesterday) : BigDecimal.ZERO;
        stats.setTotalRevenueToday(revenueToday);
        stats.setRevenueGrowth(calculateGrowth(revenueToday.doubleValue(), revenueYesterday.doubleValue()));

        stats.setDebtOrdersToday(orderRepository.countUnpaidOrdersByDate(today));

        // 2. Weekly Chart Data
        List<Object[]> rawStats = orderRepository.getSevenDayStats(startOfWeek);
        List<DashboardStatsDTO.DailyRevenueStats> weeklyStats = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E", new java.util.Locale("vi", "VN"));
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            String dayName = date.format(formatter);
            
            BigDecimal revenue = BigDecimal.ZERO;
            long count = 0;
            
            for (Object[] row : rawStats) {
                if (((LocalDate) row[0]).equals(date)) {
                    revenue = toBigDecimal(row[1]);
                    count = (Long) row[2];
                    break;
                }
            }
            weeklyStats.add(new DashboardStatsDTO.DailyRevenueStats(dayName, revenue, count));
        }
        stats.setWeeklyStats(weeklyStats);

        // 3. Recent Orders (hôm nay, 5 đơn gần nhất)
        List<Order> recentOrders = orderRepository.findRecentOrders(today, PageRequest.of(0, 5));
        stats.setRecentOrders(recentOrders.stream().map(o -> {
            String time = o.getCreatedAt() != null ? 
                DateTimeFormatter.ofPattern("HH:mm dd/MM").format(o.getCreatedAt().atZone(java.time.ZoneId.systemDefault())) 
                : "N/A";
            return new DashboardStatsDTO.RecentOrderDTO(
                o.getOrderCode(),
                o.getCustomer() != null ? o.getCustomer().getName() : "Khách vãng lai",
                o.getTotalAmount(),
                o.getStatus(),
                time
            );
        }).collect(Collectors.toList()));

        // 4. Top Customers
        List<Object[]> rawTopCustomers = customerRepository.getTopCustomers(PageRequest.of(0, 5));
        stats.setTopCustomers(rawTopCustomers.stream().map(row -> new DashboardStatsDTO.TopCustomerDTO(
            (String) row[0],
            toBigDecimal(row[1]),
            ((Long) row[2]).intValue()
        )).collect(Collectors.toList()));

        return stats;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Double) return BigDecimal.valueOf((Double) val);
        if (val instanceof Long) return BigDecimal.valueOf((Long) val);
        return BigDecimal.ZERO;
    }

    private double calculateGrowth(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }
}
