package com.safefoodtruck.sft.order.service;

import static com.safefoodtruck.sft.order.domain.OrderStatus.ACCEPTED;
import static com.safefoodtruck.sft.order.domain.OrderStatus.COMPLETED;
import static com.safefoodtruck.sft.order.domain.OrderStatus.REJECTED;

import com.safefoodtruck.sft.common.util.MemberInfo;
import com.safefoodtruck.sft.member.domain.Member;
import com.safefoodtruck.sft.member.exception.NotFoundMemberException;
import com.safefoodtruck.sft.member.repository.MemberRepository;
import com.safefoodtruck.sft.menu.domain.Menu;
import com.safefoodtruck.sft.menu.domain.MenuType;
import com.safefoodtruck.sft.menu.exception.MenuNotFoundException;
import com.safefoodtruck.sft.menu.repository.MenuRepository;
import com.safefoodtruck.sft.notification.service.NotificationService;
import com.safefoodtruck.sft.order.domain.Order;
import com.safefoodtruck.sft.order.domain.OrderMenu;
import com.safefoodtruck.sft.order.dto.request.OrderMenuRequestDto;
import com.safefoodtruck.sft.order.dto.request.OrderRegistRequestDto;
import com.safefoodtruck.sft.order.dto.response.CustomerOrderListResponseDto;
import com.safefoodtruck.sft.order.dto.response.OrderDetailResponseDto;
import com.safefoodtruck.sft.order.dto.response.OrderRegistResponseDto;
import com.safefoodtruck.sft.order.dto.response.OrderSummaryDto;
import com.safefoodtruck.sft.order.dto.response.OrderSummaryResponseDto;
import com.safefoodtruck.sft.order.dto.response.OwnerOrderListResponseDto;
import com.safefoodtruck.sft.order.dto.response.CustomerPreparingOrderListResponseDto;
import com.safefoodtruck.sft.order.dto.response.WeeklyCustomerOrderSummaryResponseDto;
import com.safefoodtruck.sft.order.exception.AlreadyCompletedOrderException;
import com.safefoodtruck.sft.order.exception.AlreadyProcessedOrderException;
import com.safefoodtruck.sft.order.exception.OrderNotPreparingException;
import com.safefoodtruck.sft.order.exception.UnAuthorizedOrderStatusUpdateException;
import com.safefoodtruck.sft.order.repository.OrderMenuRepository;
import com.safefoodtruck.sft.order.repository.OrderRepository;
import com.safefoodtruck.sft.store.domain.Store;
import com.safefoodtruck.sft.store.exception.StoreNotFoundException;
import com.safefoodtruck.sft.store.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final MenuRepository menuRepository;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public OrderRegistResponseDto registOrder(final OrderRegistRequestDto orderRegistRequestDto) {
        String email = MemberInfo.getEmail();
        Member customer = memberRepository.findByEmail(email)
            .orElseThrow(NotFoundMemberException::new);
        Store store = storeRepository.findStoreWithMenusAndImagesByStoreId(
            orderRegistRequestDto.storeId()).orElseThrow(StoreNotFoundException::new);

        Order order = Order.of(orderRegistRequestDto, customer);
        order.setStore(store);

        Order savedOrder = orderRepository.save(order);

        List<OrderMenuRequestDto> menuList = orderRegistRequestDto.menuList();
        List<OrderMenu> orderMenuList = createOrderMenus(savedOrder, menuList);

        orderMenuRepository.saveAll(orderMenuList);
        savedOrder.calculateAmount();

        notificationService.orderedSendNotify(store.getOwner().getEmail());

        return createOrderRegistResponseDto(savedOrder, menuList);
    }

    private List<OrderMenu> createOrderMenus(Order savedOrder, List<OrderMenuRequestDto> menuList) {
        return menuList.stream().map(menuRequestDto -> {
            Menu menu = menuRepository.findById(menuRequestDto.menuId())
                .orElseThrow(MenuNotFoundException::new);

            OrderMenu orderMenu = OrderMenu.builder().menu(menu).count(menuRequestDto.count())
                .build();

            savedOrder.addOrderMenu(orderMenu);
            return orderMenu;
        }).toList();
    }

    private OrderRegistResponseDto createOrderRegistResponseDto(Order savedOrder,
        List<OrderMenuRequestDto> menuList) {
        String orderTitle = createOrderTitle(menuList);
        Integer totalQuantity = menuList.stream().mapToInt(OrderMenuRequestDto::count).sum();

        int totalAmount = menuList.stream().mapToInt(menuRequestDto ->
            menuRepository.findById(menuRequestDto.menuId()).orElseThrow(MenuNotFoundException::new)
                .getPrice() * menuRequestDto.count()).sum();

        return OrderRegistResponseDto.fromEntity(savedOrder, orderTitle, totalQuantity,
            totalAmount);
    }

    private String createOrderTitle(List<OrderMenuRequestDto> menuList) {
        StringBuilder orderTitleBuilder = new StringBuilder();

        String firstMenuName = menuRepository.findById(menuList.get(0).menuId())
            .orElseThrow(MenuNotFoundException::new).getName();
        Integer firstMenuCount = menuList.get(0).count();

        String unit = MenuType.getUnitByMenuName(firstMenuName);

        orderTitleBuilder.append(firstMenuName).append(" ").append(firstMenuCount).append(unit);

        if (menuList.size() > 1) {
            orderTitleBuilder.append(" 외 ").append(menuList.size() - 1).append("개");
        }

        return orderTitleBuilder.toString();
    }


    @Transactional
    @Override
    public String acceptOrder(Integer orderId) {
        Order validOrder = getValidPendingOrder(orderId);

        validOrder.acceptOrder();
        Order savedOrder = orderRepository.save(validOrder);

        if (savedOrder.getStatus().equals(ACCEPTED.get())) {
            String orderEmail = savedOrder.getCustomer().getEmail();
            String storeName = savedOrder.getStore().getName();
            notificationService.acceptedSendNotify(orderEmail, storeName, orderId);
        }

        return savedOrder.getStatus();
    }

    @Transactional
    @Override
    public String rejectOrder(Integer orderId) {
        Order validOrder = getValidPendingOrder(orderId);

        validOrder.rejectOrder();
        Order savedOrder = orderRepository.save(validOrder);

        if (savedOrder.getStatus().equals(REJECTED.get())) {
            String orderCustomerEmail = savedOrder.getCustomer().getEmail();
            String orderStoreName = savedOrder.getStore().getName();
            notificationService.rejectedSendNotify(orderCustomerEmail, orderStoreName, orderId);
        }
        return savedOrder.getStatus();
    }

    @Transactional
    @Override
    public String completeOrder(final Integer orderId) {
        Order order = getValidPreparingOrder(orderId);

        order.complete();
        Order savedOrder = orderRepository.save(order);

        if (savedOrder.getCookingStatus().equals(COMPLETED.get())) {
            String orderCustomerEmail = savedOrder.getCustomer().getEmail();
            String orderStoreName = savedOrder.getStore().getName();
            notificationService.completedSendNotify(orderCustomerEmail, orderStoreName, orderId);
        }
        return savedOrder.getCookingStatus();
    }

    @Transactional(readOnly = true)
    @Override
    public CustomerOrderListResponseDto findCustomerOrderList() {
        String email = MemberInfo.getEmail();
        return orderRepository.findCustomerOrdersByEmail(email);

    }

    @Transactional(readOnly = true)
    @Override
    public OwnerOrderListResponseDto findStoreOrderList() {
        String email = MemberInfo.getEmail();
        List<Order> orders = orderRepository.findOrdersByStoreOwnerEmail(email);

        return OwnerOrderListResponseDto.fromEntity(orders);
    }

    @Override
    public CustomerPreparingOrderListResponseDto findAcceptedPreparingOrders() {
        String email = MemberInfo.getEmail();
        return orderRepository.findAcceptedPreparingOrders(email);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderDetailResponseDto findOrderDetail(Integer orderId) {
        Order order = orderRepository.findByOrderId(orderId);

        return OrderDetailResponseDto.fromEntity(order);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderSummaryResponseDto> getWeeklyOrderSummary() {
        String email = MemberInfo.getEmail();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        List<Order> orders = orderRepository.findOrdersByStoreOwnerEmailAndOrderTimeBetween(email,
            weekAgo.atStartOfDay(), today.atTime(23, 59, 59));

        Map<LocalDate, List<Order>> ordersGroupedByDate = orders.stream()
            .collect(Collectors.groupingBy(order -> order.getOrderTime().toLocalDate()));

        return ordersGroupedByDate.entrySet().stream().map(entry -> {
            LocalDate date = entry.getKey();
            List<Order> dailyOrders = entry.getValue();

            int totalAmount = dailyOrders.stream().mapToInt(Order::getAmount).sum();

            Map<String, Integer> menuSalesMap = new HashMap<>();
            for (Order order : dailyOrders) {
                for (OrderMenu orderMenu : order.getOrderMenuList()) {
                    String menuName = orderMenu.getMenu().getName();
                    int menuCount = orderMenu.getCount();
                    menuSalesMap.put(menuName, menuSalesMap.getOrDefault(menuName, 0) + menuCount);
                }
            }

            List<OrderSummaryDto> menuOrderSummaries = menuSalesMap.entrySet().stream()
                .map(e -> new OrderSummaryDto(e.getKey(), e.getValue())).toList();

            return new OrderSummaryResponseDto(date, totalAmount, menuOrderSummaries);
        }).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public WeeklyCustomerOrderSummaryResponseDto getWeeklyCustomerOrderSummary() {
        String email = MemberInfo.getEmail();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return orderRepository.findWeeklyCustomerOrderSummary(email, weekAgo);
    }

    private Order getValidPendingOrder(Integer orderId) {
        Order order = getOrder(orderId);
        Store loginStore = findLoginStore();

        if (!loginStore.getId().equals(order.getStore().getId())) {
            throw new UnAuthorizedOrderStatusUpdateException();
        }

        if (order.isInValidRequest()) {
            throw new AlreadyProcessedOrderException();
        }

        return order;
    }

    private Order getValidPreparingOrder(Integer orderId) {
        Order order = getOrder(orderId);
        Store loginStore = findLoginStore();

        if (!loginStore.getId().equals(order.getStore().getId())) {
            throw new UnAuthorizedOrderStatusUpdateException();
        }

        if (!order.isPreparingOrder()) {
            throw new OrderNotPreparingException();
        }

        if (order.isAlreadyCompletedOrder()) {
            throw new AlreadyCompletedOrderException();
        }

        return order;
    }

    private Order getOrder(Integer orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    private Store findLoginStore() {
        String email = MemberInfo.getEmail();
        return storeRepository.findStoreWithMenusAndImagesByOwnerEmail(email)
            .orElseThrow(StoreNotFoundException::new);
    }
}
