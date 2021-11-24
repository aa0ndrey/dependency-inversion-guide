package aa0ndrey.dependency_inversion_guide.step_14.application.order;

import aa0ndrey.dependency_inversion_guide.step_14.application.time_span_manager.TimeSpanManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrderRepositoryObserverImpl {
    private final TimeSpanManager timeSpanManager;

    public void afterSqlCreated(String sql) {
        timeSpanManager.addEvent("Создан sql запрос для вставки заказа в таблицу: " + sql);
    }
}
