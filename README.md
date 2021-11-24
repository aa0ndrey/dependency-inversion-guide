## Подробное руководство по инверсии зависимостей. Часть 1

Инверсия зависимостей - один из принципов SOLID, который лежит в основе построения гексагональной архитектуры
приложения. Существует множество статей, которые раскрывают суть принципа и объясняют как его применять. И, возможно,
читатель уже знаком с ними. Но в рамках данной статьи будет продемонстрирован подробный разбор "тактических" приемов для
успешного использования инверсии зависимостей и, возможно, в этом смысле даже искушенный читатель сможет найти для себя
что-то новое. Примеры представлены на языке программирования Java с соответствующим окружением, но при этом для чтения
достаточно понимания похожих языков программирования.

### 0. Проблема взаимодействия изолируемого модуля с инфраструктурным

Пусть необходимо автоматизировать процесс, который позволяет создавать от пользователя заказы на покупку товара. И в
рамках этого процесса необходимо проверять баланс пользователя. Баланс должен быть больше, чем стоимость товара.

В начале рассмотрим классы-данных, соответствующие процессу:

`CreateOrderRequest` - класс-данных запроса, который отправляет пользователь для создания заказа на товар

```java
package aa0ndrey.dependency_inversion_guide.step_0.core.order;

public class CreateOrderRequest {
    private UUID userId;
    private UUID productId;
}
```

`User` - класс-данных пользователя

```java
package aa0ndrey.dependency_inversion_guide.step_0.core.user;

public class User {
    private UUID id;
    private String name;
    private int balance;
}
```

`Product` - класс-данных товара

```java
package aa0ndrey.dependency_inversion_guide.step_0.core.product;

public class Product {
    private UUID id;
    private String title;
    private int price;
}
```

`Order` - класс-данных заказа

```java
package aa0ndrey.dependency_inversion_guide.step_0.core.order;

public class Order {
    private UUID id;
    private UUID userId;
    private UUID productId;
}
```

Для работы с базой данных понадобятся классы репозиториев, которые позволят сохранять и получать данные:

```java
package aa0ndrey.dependency_inversion_guide.step_0.postgres.user;

public class UserRepositoryImpl {
    public User find(UUID id) {
        //реализация select * from user where user.id = ?
    }
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_0.postgres.product;

public class ProductRepositoryImpl {
    public Product find(UUID id) {
        //реализация select * from product where product.id = ?
    }
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_0.postgres.order;

public class OrderRepositoryImpl {
    public void create(Order order) {
        //реализация insert into order (id, user_id, product_id) values (?, ?, ?)
    }
}
```

И в заключении рассмотрим класс сервиса, который будет содержать в себе основную логику, описанную ранее

```java
package aa0ndrey.dependency_inversion_guide.step_0.core.order;

import aa0ndrey.dependency_inversion_guide.step_0.postgres.order.OrderRepositoryImpl;
import aa0ndrey.dependency_inversion_guide.step_0.postgres.product.ProductRepositoryImpl;
import aa0ndrey.dependency_inversion_guide.step_0.postgres.user.UserRepositoryImpl;

public class OrderService {
    private final UserRepositoryImpl userRepository;
    private final ProductRepositoryImpl productRepository;
    private final OrderRepositoryImpl orderRepository;

    public void create(CreateOrderRequest request) {
        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);
    }
}
```

Теперь предположим, что необходимо разложить представленные классы по двум модулям: core и postgres. Ожидается, что
модуль core будет содержать в себе основную логику приложения, которую необходимо изолировать от инфраструктурных
зависимостей. В свою очередь в postgres модуле ожидается, что будет размещен весь код, связанный с работой с базой
данных postgres.

Важным нюансом является то, что postgres модуль должен зависеть от core модуля, а core модуль **не должен** зависеть от
postgres модуля. Postgres и core модули будут собраны с помощью maven как отдельные maven модули.
[Maven](https://maven.apache.org/what-is-maven.html) - это инструмент, предназначенный для сборки проекта и управления
зависимостями в проекте. Ниже будут представлены конфигурационные pom.xml файлы для core и postgres модуля.

Файл pom.xml, содержащий конфигурацию core модуля

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>aa0ndrey</groupId>
    <artifactId>dependency-inversion-guide-step-0-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</project>
```

Файл pom.xml, содержащий конфигурацию postgres модуля

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>aa0ndrey</groupId>
    <artifactId>dependency-inversion-guide-step-0-postgres</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>aa0ndrey</groupId>
            <artifactId>dependency-inversion-guide-step-0-core</artifactId> <!--(1)-->
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

Если вы не знакомы с maven, то важным, на что необходимо обратить внимание, является блок
`<dependencies></dependecies>`. В postgres модуле есть ссылка на core модуль. А в core модуле нет
блока `<dependencies></dependecies>` совсем, что в частности означает что нет зависимости на модуль postgres. Важно
отметить, что maven не позволит скомпилировать модуль, ссылающийся на код модуля, которого нет в зависимостях. Также в
maven запрещены циклические зависимости, то есть, если postgres модуль зависит от core модуля, то core модуль не может
зависеть от postgres модуля.

В файле конфигурации pom.xml для postgres модуля есть комментарий `<!--(1)-->` здесь и далее комментарии будут
использованы, в том числе для указания ссылок на код. При этом ссылка на код оформляется в блоке кода с помощью круглых
скобок, внутри которых указана цифра. На эту цифру в основном тексте будет ссылка. Например, так: в файле pom.xml для
postgres модуля в `(1)` указана зависимость на core модуль.

Теперь разложим рассмотренные ранее классы по модулям core и postgres.

Файловая структура core модуля

```text
├── order
│   ├── CreateOrderRequest.java
│   ├── Order.java
│   └── OrderService.java
├── product
│   └── Product.java
└── user
    └── User.java
```

Файловая структура postgres модуля

```text
├── order
│   └── OrderRepositoryImpl.java
├── product
│   └── ProductRepositoryImpl.java
└── user
    └── UserRepositoryImpl.java
```

На самом деле как разложены классы внутри модулей, можно было понять ранее. В примерах кода всюду указаны пакеты
`package`. При именовании пакетов после `aa0ndrey.dependency_inversion_guide` указывается номер рассматриваемого
примера, в данном случае `step-0`, и затем указывается имя модуля, в котором расположен класс. Например, класс, с
указанным `package aa0ndrey.dependency_inversion_guide.step_0.core.order`, находится в модуле core.

Если попробовать собрать эти два модуля, то при сборке модуля core возникнет ошибка, связанная с тем, что в классе
`OrderService` используются `OrderRepositoryImpl`, `ProductRepositoryImpl` и `UserRepositoryImpl`, которых нет в модуле
core и в подключаемых зависимостях, потому что классы этих репозиториев находятся в модуле postgres, на который
невозможно и, что самое главное **не нужно** создавать зависимость в модуле core. И тут становится понятна проблема,
которая возникает при желании изолировать модуль от другого инфраструктурного модуля, из которого по коду необходимо
получать и в который необходимо отправлять данные. Так как же это сделать? Решение будет рассмотрено в следующем
разделе.

### 1. Инверсия зависимостей с помощью интерфейсов

Чтобы решить проблему, обозначенную в предыдущем разделе, достаточно в core модуле создать интерфейсы для репозиториев,
а сами репозитории из postgres модуля заставить реализовывать указанные интерфейсы.  
Добавим интерфейсы репозиториев в core модуль

```java
package aa0ndrey.dependency_inversion_guide.step_1.core.user;

public interface UserRepository {
    User find(UUID id);
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_1.core.product;

public interface ProductRepository {
    Product find(UUID id);
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_1.core.order;

public interface OrderRepository {
    void create(Order order);
}
```

Затем укажем реализацию этих репозиториев в postgres модуле

```java
package aa0ndrey.dependency_inversion_guide.step_1.postgres.user;

public class UserRepositoryImpl implements UserRepository {
    @Override
    public User find(UUID id) {
        //реализация select * from user where user.id = ?
    }
}

```

```java
package aa0ndrey.dependency_inversion_guide.step_1.postgres.product;

public class ProductRepositoryImpl implements ProductRepository {
    @Override
    public Product find(UUID id) {
        //реализация select * from product where product.id = ?
    }
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_1.postgres.order;

public class OrderRepositoryImpl implements OrderRepository {
    @Override
    public void create(Order order) {
        //реализация insert into order (id, user_id, product_id) values (?, ?, ?)
    }
}
```

И в заключении перепишем логику в OrderService так, чтобы он начал использовать вместо UserRepositoryImpl,
ProductRepositoryImpl и OrderRepositoryImpl их интерфейсы, которые расположены в модуле core.

```java
package aa0ndrey.dependency_inversion_guide.step_1.core.order;

public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public void create(CreateOrderRequest request) {
        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);
    }
}
```

Файловая структура core модуля

```text
├── order
│   ├── CreateOrderRequest.java
│   ├── Order.java
│   ├── OrderRepository.java (1)
│   └── OrderService.java
├── product
│   ├── Product.java
│   └── ProductRepository.java (2)
└── user
    ├── User.java
    └── UserRepository.java (3)
```

Файловая структура postgres модуля

```text
├── order
│   └── OrderRepositoryImpl.java (4)
├── product
│   └── ProductRepositoryImpl.java (5)
└── user
    └── UserRepositoryImpl.java (6)
```

Этими изменениями удалось достигнуть того, что core модуль перестал использовать какие-либо классы из postgres модуля. И
теперь оба модуля могут быть скомпилированы. Вместо использования прямых зависимостей на классы из инфраструктурного
модуля (postgres) `(4)`, `(5)`, `(6)` можно создавать интерфейсы `(1)`, `(2)`, `(3)` на эти классы в изолируемом
модуле (core), которые должны быть реализованы в инфраструктурном модуле (postgres). При этом нет никаких проблем в том,
что классы из инфраструктурного модуля (postgres) зависят от классов из изолируемого модуля (core).

```text
  ┌────────────────┐
  │      Core      │
  │┌──────────────┐│
  ││UserRepository│◄───────┐
  │└──────────────┘│       │
  └────────────────┘       │
┌────────────────────┐     │
│      Postgres      │     │
│┌──────────────────┐│     │
││UserRepositoryImpl│┼─────┘
│└──────────────────┘│
└────────────────────┘
```

Важно понимать и идейную составляющую данного приема. Она заключается в том, что изолируемый модуль (core)
предъявляет требования к реализации с помощью интерфейса. То есть "главным" в этом отношении является интерфейс, под
который подстраивается реализация.

Возможно, на этом моменте другие руководства по инверсии зависимостей подходят к завершению. Но в рамках данного
руководства это только начало.

### 2. Проблема использования интерфейсов, раскрывающих инфраструктуру

Прежде чем перейти к рассмотрению следующей проблемы, стоит отметить, что здесь и далее большинство приведенных примеров
очень сильно упрощены, особенно с точки зрения технической реализации. Это сделано намерено для того, чтобы не засорят
ненужными деталями рассматриваемые примеры.

Предположим теперь, что есть необходимость управлять транзакциями. И пусть для этого достаточно уметь открывать и
фиксировать транзакцию. И также пусть необходимо открыть транзакцию перед получением любых данных из базы данных, а
зафиксировать транзакцию необходимо только после последнего обращения к базе данных.

Тогда можно в модуле core создать интерфейс, который позволяет управлять транзакциями.

```java
package aa0ndrey.dependency_inversion_guide.step_2.core.transaction_manager;

public interface TransactionManager {
    void begin();

    void commit();
}
```

В свою очередь реализацию этого интерфейса стоит поместить в модуль postgres

```java
package aa0ndrey.dependency_inversion_guide.step_2.postgres.transaction_manager;

public class TransactionManagerImpl implements TransactionManager {
    public void begin() {
        //реализация начала транзакции
    }

    public void commit() {
        //реализация фиксации транзакции
    }
}
```

И теперь появляется возможность добавить использование интерфейса `TransactionManager` в класс с основной логикой
`OrderService`.

```java
package aa0ndrey.dependency_inversion_guide.step_2.core.order;

public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TransactionManager transactionManager;

    public void create(CreateOrderRequest request) {
        transactionManager.begin(); //(1)

        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);

        transactionManager.commit(); //(2)
    }
}
```

В классе `OrderService` в `(1)` транзакция открывается, а в `(2)` происходит ее фиксация. Вообще говоря, код из `(2)`
нужно было бы написать в конструкции try-catch-finally, также при этом используя rollback в случае исключения, но
напомню, что примеры намерено упрощены. А еще лучше было бы использовать аннотацию `@Transactional` из какого-либо
фреймворка, но это не является предметом для обсуждения в данной статье. Использование упрощенной версии
интерфейса `TransactionManager` мотивировано тем, что он хорошо известен, по крайней мере Java-разработчикам, и с его
участием удобно рассматривать большинство примеров.

Проблема данного решения заключается в том, что управление транзакциями достаточно специфичный механизм по отношению к
основной логике. Несмотря на то, что детали реализации скрываются с помощью интерфейса, сам интерфейс рассказывает о
каких-то инфраструктурных особенностях. По этой причине наличие такого интерфейса крайне нежелательно.

Тут важно понимать мотивацию, зачем необходимо избегать подобных интерфейсов в построении изолируемого модуля (core).
Представим, что есть какая-то конкретная библиотека, например, для работы с mysql. Тогда можно взять и создать на каждый
класс для этой библиотеки интерфейс в изолируемом модуле и использовать эти интерфейсы в основной логике внутри
изолируемого модуля (core). Но в этом случае потеряются все преимущества от того, что используются интерфейсы.

Например, одной из причин для изолирования модуля от инфраструктуры является обобщение и упрощение использования сложных
инфраструктурных модулей для того, чтобы не утопать в их деталях при написании основного кода. Но так как интерфейсы
один в один будут повторять классы библиотеки, то в этом смысле не получится добиться какого-либо упрощения.

Также интерфейсы используются для того, чтобы иметь потенциальную возможность подменять решения из группы альтернатив.
Но это будет возможно только, если альтернативу можно будет адаптировать к уже существующим интерфейсам. А если они
являются точной копией другой используемой библиотеки, то, скорее всего, пропадает всякая возможность замены.

Несмотря на то, что для представленного интерфейса `TransactionManager` можно было бы создать реализацию для большого
множества баз данных, данный интерфейс раскрывает детали реализации взаимодействия с базой данных и в этом смысле
является чужеродным по отношению к основной логике в изолируемом модуле (core). И поэтому от него необходимо избавиться.

### 3. Инверсия зависимостей с помощью шаблона проектирования наблюдатель

В данном разделе будет предложено решение проблемы из предыдущего раздела. Вместо того чтобы использовать интерфейсы,
раскрывающие инфраструктурные особенности, можно использовать шаблон проектирования наблюдатель (observer). Идея
заключается в том, чтобы для класса `OrderService` создать общий интерфейс для наблюдателей, которые смогут обрабатывать
общие события. При этом сами события будут отправляться в те моменты, когда должен был быть вызван нежелательный
раскрывающий детали интерфейс (в данном случае `TransactionManager`).

Создадим для начала классы событий: `CreateOrderEvents.Start` и `CreateOrderEvents.End`

```java
package aa0ndrey.dependency_inversion_guide.step_3.core.order;

public class CreateOrderEvents {
    public static class Start {
        private CreateOrderRequest request; //(1)
    }

    public static class End {
        private CreateOrderRequest request; //(2)
        private User user; //(3)
        private Product product; //(4)
        private Order order; //(5)
    }
}
```

При выборе названия события можно отталкиваться от названия места по ходу выполнения основной логики в OrderService.
Например, событие `CreateOrderEvents.Start` будет отправляться в самом начале метода при создании заказа. Поэтому оно
называется Start. В свою очередь событие `CreateOrderEvents.End` отправляется в конце метода и поэтому оно называется
End. Если бы понадобилось отправлять событие перед проверкой баланса пользователя, то можно было бы назвать событие
BeforeCheckUserBalance.

Тут важным является не дать название событием так, чтобы по названию можно было бы понять, какая обработка ожидает эти
события. Иначе тем самым будут раскрыты детали реализации. Например, такие названия событий не подойдут:
`OnTransactionBegin` и `OnTransactionCommit`.

Также стоит отметить, что события необходимо наполнить не конкретными данными, которые будут необходимы конкретному
наблюдателю. А общими данными, которые доступны в момент отправки события. Поэтому для события
`CreateOrderEvents.Start` указывается только поле с запросом `(1)`, так как только оно доступно в начале метода. В свою
очередь в конце выполнения метода доступны: исходный запрос `(2)`, полученный пользователь `(3)`, полученный
продукт `(4)`, а также созданный заказ `(5)`.

Это все делается для того, чтобы потенциально наблюдателем этих событий мог быть какой угодно другой инфраструктурный
модуль. Причем это может быть необязательно возможная альтернатива текущему модулю. Например, одно и то же событие может
обрабатываться модулем базы данных для того, чтобы начать транзакцию, и модулем HTTP клиента, для того, чтобы
осуществить получение токена авторизации.

Далее можно создать общий интерфейс для наблюдателя:

```java
package aa0ndrey.dependency_inversion_guide.step_3.core.order;

public interface CreateOrderObserver {
    void onStart(CreateOrderEvents.Start event);

    void onEnd(CreateOrderEvents.End event);
}
```

И затем можно изменить класс `OrderService`

```java
package aa0ndrey.dependency_inversion_guide.step_3.core.order;

public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final List<CreateOrderObserver> observers; //(6)

    public void create(CreateOrderRequest request) {
        var startEvent = new CreateOrderEvents.Start(request); //(7)
        observers.forEach(observer -> observer.onStart(startEvent)); //(8)

        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);

        var endEvent = new CreateOrderEvents.End( //(9)
                request,
                user,
                product,
                order
        );
        observers.forEach(observer -> observer.onEnd(endEvent)); //(10)
    }
}
```

На что стоит обратить внимание? Было удалено всякое использование интерфейса `TransactionManager` более того, он был
удален из модуля core. Вместо него теперь используется `CreateOrderObserver` в `(6)`. Для этого в начале метода
создаётся событие `CreateOrderEvents.Start` в `(7)` и затем оно отправляется наблюдателям в `(8)`. Аналогично в конце
метода создаётся событие `CreateOrderEvents.End` в `(9)` и отправляется в `(10)`.

И затем можно добавить реализацию наблюдателя в postgres модуль

```java
package aa0ndrey.dependency_inversion_guide.step_3.postgres.order;

public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;

    @Override
    public void onStart(CreateOrderEvents.Start event) {
        transactionManagerImpl.begin();
    }

    @Override
    public void onEnd(CreateOrderEvents.End event) {
        transactionManagerImpl.commit();
    }
}
```

Стоит обратить внимание на то, что внутри уже реализации наблюдателя происходит обращение к `TransactionManagerImpl` для
того, чтобы решить задачу по управлению транзакциями.

Теперь приведём текущую файловую структуру модуля core

```text
├── order
│   ├── CreateOrderEvents.java
│   ├── CreateOrderRequest.java
│   ├── CreateOrderObserver.java
│   ├── Order.java
│   ├── OrderRepository.java
│   └── OrderService.java
├── product
│   ├── Product.java
│   └── ProductRepository.java
└── user
    ├── User.java
    └── UserRepository.java
```

И модуля postgres

```text
├── order
│   ├── CreateOrderObserverImpl.java
│   └── OrderRepositoryImpl.java
├── product
│   └── ProductRepositoryImpl.java
├── transaction_manager
│   └── TransactionManagerImpl.java
└── user
    └── UserRepositoryImpl.java
```

Чего в итоге удалось добиться? Удалось полностью избавиться от интерфейса `TranasactionManager`, который частично
раскрывал детали реализации инфраструктурного модуля (postgres). И удалось это сделать за счет добавления отправки
событий. Стоит отметить, что таких отправок событий можно добавить сколько угодно по ходу метода. И тем самым у
разработчика появляется общий механизм для расширения основной логики находящейся в (core) модуле. И с помощью этого
можно внедрять произвольную логику, находящуюся в инфраструктурном модуле (postgres). Важно отметить, что такие отправки
событий можно использовать не обязательно только в начале и конце метода, но и где-то посередине. Эту особенность стоит
запомнить, так как в будущих разделах, когда будет рассмотрен промежуточный модуль (application), будут предложены
другие решения, которые не позволяют внедрить какое-либо расширение посередине метода.

Интересно также отметить то, что на самом деле данное решение является небольшой модификацией стандартной инверсии
зависимостей. В стандартном решении используется конкретный интерфейс, например, `TransactionManager` и вызываются его
конкретные методы `begin` и `commit`, и передаются конкретные данные, но для методов `begin` и `commit` их просто нет. В
решении через использование шаблона наблюдатель, интерфейс обобщается в смысле названия и в данном случае
это `CreateOrderObserver`. И также обобщаются его методы и передаваемые данные. Методы становятся `onStart` и `onEnd`, а
передаваемые данные, то есть события, как это было показано ранее, содержат общие для всех наблюдателей поля.

### 4. Использование контекста вместо событий

Предположим теперь, что реализация TransactionManagerImpl изменилась.

```java
package aa0ndrey.dependency_inversion_guide.step_4.postgres.transaction_manager;

public class TransactionManagerImpl {
    public long begin() { //(1)
        //реализация начала транзакции
    }

    public void commit(long transactionId) { //(2)
        //реализация фиксации транзакции
    }
}
```

Теперь в `(1)` метод `begin` возвращает id текущей транзакции. В свою очередь, в `(2)` метод `commit` принимает id
транзакции, которую необходимо зафиксировать. То есть теперь появилась потребность передать данные от одного
инфраструктурного вызова к другому. Но как это сделать, если теперь эти вызовы осуществляются изолировано в рамках
обработки событий по шаблону проектирования наблюдатель? Если проблема не стала ясна, то для понимания рекомендуется
вернуться к классам `OrderService` и `CreateOrderObserverImpl` из предыдущего раздела 3 и попытаться представить
решение, которое позволит передать id транзакции от метода `begin` в метод `commit`.

Стоит отметить, что подобная проблема не возникла, если бы не было принято решение отказаться от интерфейса
`TransactionManager`. Правда, в этом случае в методе с основной логикой в изолируемом модуле (core) начала бы появляться
дополнительная логика по работе с транзакциями. То есть основной код начал бы сильнее обрастать инфраструктурными
деталями, так как в него была бы добавлена еще и логика по запоминанию id транзакции, чтобы затем использовать этот id
для фиксации транзакции.

Как же решить возникшую проблему? Для этого можно воспользоваться контекстом. Контекст - это класс-данных, который
содержит необходимую информацию о выполняющемся процессе. Ниже приведен пример контекста, который соответствует процессу
создания заказа.

```java
package aa0ndrey.dependency_inversion_guide.step_4.core.order;

public class CreateOrderContext {
    private CreateOrderRequest request; //(3)
    private User user; //(4)
    private Product product; //(5)
    private Order createdOrder; //(6)

    private Map<String, Object> data; //(7)
}
```

`CreateOrderContext` содержит в `(3)` поле исходного запроса, в `(4)` найденного пользователя, в `(5)` найденный
продукт, в `(6)` созданный заказ. Состав полей очень похож на событие `CreateOrderEvents.End` за исключением поля `(7)`.
Это поле как раз сейчас и понадобится.

Идея в том, что все поля, которые известны основному процессу, находящемуся в изолируемом модуле (core), имеют строгий
формат в контексте с конкретными именами, потому что они известны этому процессу. В свою очередь поле `data` в `(7)`
представлено ассоциативным массивом, где в качестве ключа используется строка, а в качестве значения используется
Object, то есть самый общий тип данных в Java, наследниками которого являются все остальные классы. Это позволяет в
поле `data` записывать совершенно произвольные данные в динамическом формате не фиксируя структуру. А это значит, что
записывая туда какие-либо инфраструктурные данные, не создается зависимости в основном коде от инфраструктуры, до тех
пор пока в нем, в основном коде, не используется поле `data`, что делать не рекомендуется.

Теперь изменим интерфейс `CreateOrderObserver` таким образом, чтобы вместо событий методы принимали контекст.

```java
package aa0ndrey.dependency_inversion_guide.step_4.core.order;

public interface CreateOrderObserver {
    void onStart(CreateOrderContext context);

    void onEnd(CreateOrderContext context);
}
```

На самом деле можно считать, что события существуют, только они все одинаковые и имеют лишь одно поле и это поле всегда
контекст. Поэтому, возможно, не имеет смысла явно создавать отдельные классы для каждого такого события.

Теперь изменим логику в `OrderService` так, чтобы использовался контекст

```java
package aa0ndrey.dependency_inversion_guide.step_4.core.order;

public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final List<CreateOrderObserver> observers;

    public void create(CreateOrderContext context) {
        observers.forEach(observer -> observer.onStart(context));

        var request = context.getRequest();
        var user = userRepository.find(request.getUserId());
        context.setUser(user); //(8)
        var product = productRepository.find(request.getProductId());
        context.setProduct(product); //(9)

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);
        context.setCreatedOrder(order); //(10)

        observers.forEach(observer -> observer.onEnd(context));
    }
}
```

Необходимо отметить, что каждый раз когда появляются новые __важные__ объекты, в методе `create` они обязательно
добавляются в контекст в `(8)`, `(9)`, `(10)`. Зачем это делается? А это на самом деле делается аналогично использованию
событий. При разработке основной логики нет знания о том, как наблюдатель будет реагировать на то или иное событие. И
потенциально ему для работы могут понадобиться данные, которые появляются в основной логике.

И теперь можно перейти к самому важному, к изменению реализации интерфейса `CreateOrderObserver`, то есть к
классу `CreateOrderObserverImpl`.

```java
package aa0ndrey.dependency_inversion_guide.step_4.postgres.order;

public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;

    @Override
    public void onStart(CreateOrderContext context) {
        var transactionId = transactionManagerImpl.begin();
        context.getData().put("transaction-id", transactionId); //(11) 
    }

    @Override
    public void onEnd(CreateOrderContext context) {
        var transactionId = (Long) context.getData().get("transaction-id"); //(12)
        transactionManagerImpl.commit(transactionId);
    }
}
```

Тут все устроено следующем образом. При открытии транзакции в методе `onStart` в `(11)` id транзакции сохраняется в
контексте в поле `data` по ключу `transaction-id`. Затем уже в методе `onEnd`, когда необходимо зафиксировать
транзакцию, в `(12)` из контекста по этому же самому ключу извлекается id транзакции, который затем используется для её
фиксации.

Чего в итоге удалось добиться? За счет использования контекста между двумя изолированными методами для обработки событий
внутри инфраструктурного модуля удалось передать данные. При этом за счёт обобщенного поля `data` внутри контекста
детали реализации инфраструктурного модуля не проникли в изолируемый модуль (core). Тут является очень важным, что в
контексте не появилось конкретного поля, такого как `long transactionId`, которое раскрывало бы детали реализации.

### 5. Альтернативы полю data из контекста

В данном разделе будут обсуждаться альтернативы полю `data` из контекста для того, чтобы передавать данные из одного
инфраструктурного метода в другой. В Java есть механизм, позволяющий привязать данные к потоку. Разрабатываемое
приложение может быть построено так, что для каждого обрабатываемого запроса выделяется отдельный поток. И этот поток
может эксклюзивно использоваться логикой обработки, до тех пор пока она не завершится. В этом случае можно использовать
`ThreadLocal` в качестве альтернативы полю `data`.

Рассмотрим, как можно изменить `CreateOrderObserverImpl` для того, чтобы использовать `ThreadLocal` вместо `data`

```java
package aa0ndrey.dependency_inversion_guide.step_5.postgres.order;

public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;
    private final ThreadLocal<Long> transactionId = new ThreadLocal<>(); //(1)

    @Override
    public void onStart(CreateOrderContext context) {
        transactionId.set(transactionManagerImpl.begin());
    }

    @Override
    public void onEnd(CreateOrderContext context) {
        transactionManagerImpl.commit(transactionId.get());
    }
}
```

Для этого достаточно завести поле `transactionId`, как это сделано в `(1)`. И затем можно рассмотреть, что происходит в
самих методах. При вызове метода `onStart` происходит запись id транзакции в поле `transactionId`, а в методе `onEnd`
значение извлекается из поля `transactionId` для того, чтобы зафиксировать транзакцию. Сигнатуры методов `onStart`
и `onEnd` остались без изменений и все также принимают контекст, но в данном случае контекст совсем не используется.

Может возникнуть вопрос, а почему ранее нельзя было создать переменную `long transactionId` и использовать ее
аналогично? Предполагалось, что каждый класс сервиса, репозитория и наблюдателя имеет по одному экземпляру в работающем
приложении, то есть они реализуют шаблон одиночка (singleton). И в этом случае один и тот же экземпляр класса может
параллельно использоваться в нескольких потоках, и потоки могут друг другу мешать, конкурируя за единственное поле.
Поэтому прямое использование переменной типа `long` не подойдёт. В свою очередь, тип `ThreadLocal` будет гарантировать,
что каждый поток будет работать со своим значением.

Суть данного примера не в том, чтобы показать, как именно в Java через `ThreadLocal` можно решить поставленную проблему,
а в том, что, если в используемом фреймворке или библиотеке или языке программирования есть механизм, позволяющий
привязать данные к процессу обработки, то это можно использовать для того, чтобы передавать данные между изолированными
инфраструктурными методами такими как `onStart` и  `onEnd` в `CreateOrderObserverImpl`.

Но если говорить конкретно про Java и `ThreadLocal`, то тут необходимо быть осторожным и всегда рассматривать
альтернативу с полем `data` из контекста. `ThreadLocal` может доставить неудобства, если понадобится в рамках обработки
дополнительно создать потоки. Также некоторые новые реактивные фреймворки могут не давать гарантии того, что вся логика
будет обрабатываться одним потоком для одного запроса. Передавая все в контексте, разработчик получает полный контроль
над данными.

### 6. Передача данных от инфраструктурного модуля в изолируемый модуль

Что если теперь потребуется отправлять данные не только из изолируемого модуля (core) в инфраструктурный модуль
(postgres), но и обратно из инфраструктурного в изолируемый. Такая необходимость тоже может возникнуть. Но в рамках
данного раздела будут предъявлены самые ультимативные требования. Пусть в основной логике в классе `OrderService`
необходимо отказаться от использования всех интерфейсов репозиториев. Для этого на самом деле уже все есть. В данном
случае достаточно использовать уже созданный ранее контекст со всеми его полями.

Для начала рассмотрим изменения, внесенные в класс `OrderService`

```java
package aa0ndrey.dependency_inversion_guide.step_6.core.order;

public class OrderService {
    private final List<CreateOrderObserver> observers;

    public void create(CreateOrderContext context) {
        observers.forEach(observer -> observer.onStart(context));

        var user = context.getUser(); //(1)
        var product = context.getProduct(); //(2)

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        context.setCreatedOrder(order); //(3)

        observers.forEach(observer -> observer.onEnd(context));
    }
}
```

Теперь вместо использования репозиториев напрямую для получения данных, необходимых для выполнения основной логики,
используется контекст. То есть теперь в `(1)` и `(2)` информация о пользователе и продукте извлекается из контекста. В
свою очередь вместо вызова метода репозитория для сохранения созданного заказа используется контекст, в который
добавляется только что созданный заказ в `(3)`. Стоит отметить, что метод `create` стал меньше и проще.

Но чтобы это работало, необходимо внести изменения в класс `CreateOrderObserverImpl`

```java
package aa0ndrey.dependency_inversion_guide.step_6.postgres.order;

public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;
    private final ThreadLocal<Long> transactionId = new ThreadLocal<>();
    private final UserRepositoryImpl userRepository;
    private final ProductRepositoryImpl productRepository;
    private final OrderRepositoryImpl orderRepository;

    @Override
    public void onStart(CreateOrderContext context) {
        transactionId.set(transactionManagerImpl.begin());
        var request = context.getRequest();
        context.setUser(userRepository.find(request.getUserId())); //(4)
        context.setProduct(productRepository.find(request.getProductId())); //(5)
    }

    @Override
    public void onEnd(CreateOrderContext context) {
        transactionManagerImpl.commit(transactionId.get());
        orderRepository.create(context.getCreatedOrder()); //(6)
    }
}
```

И здесь как раз в `(4)`, `(5)` и `(6)` добавляются все вызовы репозиториев, которые были вынесены из `OrderService`.

В итоге с помощью внесенных изменений удалось получить возможность передачи данных от инфраструктурного модуля
(postgres) в изолируемый модуль (core). Тут стоит обратить внимание, что для этого использовались именно конкретные поля
контекста, такие как `user` и `product`, а не обобщенное поле `data`. Это связано стем, что, во-первых, основной логике
известны сущности `user` и `product`, а, во-вторых, в основной логике не должно использоваться поле `data`.

Подведем итог. С помощью шаблона наблюдатель с использованием контекста вместо отдельных событий, возможно передавать
данные по всем трем направлениям:

- из изолируемого модуля (core) в инфраструктурный модуль (postgres)
- из инфраструктурного модуля (postgres) в изолируемый модуль (core)
- между методами обработки событий инфраструктурного модуля.

При этом для отправки из изолируемого модуля (core) в инфраструктурный модуль (postgres) и обратно используются
конкретные поля, а для организации взаимодействия между методами обработки событий инфраструктурного модуля используется
обобщенное поле `data`, либо механизмы, обеспечивающие привязку данных к процессу выполнения, такие как `ThreadLocal`. И
не стоит также забывать о том, что точек для отправки событий с использованием контекста можно добавить сколько угодно в
метод с основной логикой, даже посередине, а не только в начале и конце. При этом не придется даже создавать отдельные
события, так как всю необходимую информацию о процессе содержит сам контекст.

После рассмотрения предыдущих разделов какие можно сделать выводы, уточнения и предостережения? Во-первых, стоит
отметить, что предложенные примеры являются искусственными, и не следует их воспринимать буквально. То есть не стоит
отказываться от интерфейсов для репозиториев в пользу использования контекста с шаблоном наблюдатель, как это было
сделано в последнем разделе, если для этого нет каких-либо дополнительных причин. Во-вторых, не стоит прибегать к
реализации использования `TransactionManager`, как это было продемонстрировано ранее, потому что это и не самый удачный
подход конкретно для `TransactionManager`. Это можно будет увидеть в следующих разделах, когда будет продемонстрировано
использование промежуточного модуля (application). `TransactionManager` был использован в примерах только потому, что с
ним можно было продемонстрировать все возможные приемы, сохраняя при этом размеры примеров и обоснования для
использования относительно небольшими и почти реалистичными.

Что является ценным из предыдущих разделов, так это сами приемы. Для типичной ситуации инверсии зависимостей, достаточно
использования интерфейса для репозитория или клиента, помещаемого в изолируемый модуль (core). Если в основной логике
возникает потребность вызова методов из инфраструктурных модулей, которые раскрывают детали реализации даже при
использовании интерфейсов, то именно в этом случае стоит использовать шаблон наблюдатель. И далее в зависимости от того,
в каком направлении необходимо организовать передачу данных, можно использовать то или иное решение. Универсальным, но
не всегда оптимальным, механизмом для передачи данных является использование контекста.

Итоговый список приемов:

- интерфейс, отделенный от реализации - стандартный прием инверсии зависимостей, для случаев, когда интерфейс скрывает
  прямое использование общих классов, которые __не богаты__ инфраструктурными деталями, таких как репозитории для базы
  данных или http клиенты и другие.
- шаблон наблюдатель - прием инверсии зависимостей, который скрывает прямое использование классов и их методов. Возможны
  следующие варианты использования шаблона:
    - стандартный с событиями - передача данных только от изолируемого модуля в инфраструктурный модуль.
    - с контекстом без поля `data` - передача данных от изолируемого модуля в инфраструктурный модуль и обратно
    - с привязкой данных к процессу выполнения - передача данных между изолируемыми инфраструктурными методами обработки
      событий
    - с контекстом с полем `data` - передача данных от изолируемого модуля в инфраструктурный модуль и обратно, а также
      между изолируемыми инфраструктурными методами обработки событий

В следующей части (или следующих частях) руководства будет продемонстрировано использование промежуточного модуля
(application) и будет определена область его ответственности. Также будет представлен способ организации кода,
позволяющий достичь того, что методы основной логики из изолируемого модуля (core) будут удовлетворять критериям
"чистых" функций. И также будет представлен способ разделения процесса на этапы, в частности для организации
взаимодействия через очередь сообщений.

В заключении для лучшего погружения в материал читателю предлагается решить задачу.

#### Задача

Пусть для каждого запроса при взаимодействии с базой данных необходимо также передавать id транзакции, который
появляется при вызове метода `begin` из `TransactionManagerImpl`. То есть необходимо внести некоторые изменения, как
минимум, в классы `UserRepositoryImpl`, `ProductRepositoryImpl`, `OrderRepositoryImpl`. Какие приемы и как их применить
для того, чтобы удовлетворить поставленному требованию, при этом не добавив лишних инфраструктурных зависимостей в
основную логику в изолируемый модуль (core)? Приведите как можно больше возможных вариантов решения поставленной задачи.

#### Возможные решения

Заметим, что данные необходимо передавать между изолированными инфраструктурными методами.

1. Использовать механизм привязки данных к выполняющемуся процессу, например, `ThreadLocal`. При этом сам id транзакции
   будет некорректно хранить внутри `CreateOrderObserverImpl`, так как репозитории потенциально могут быть
   переиспользованы в нескольких местах и поэтому они не могут быть привязаны к конкретному наблюдателю. Для этого
   достаточно завести отдельный класс, который будет хранить информацию о транзакции. Например, `TransactionInfo` с
   полем `ThreadLocal<Long> transactionId`. При этом в реализации `TransactionManagerImpl` можно затребовать, чтобы id
   транзакции сохранялся в `TransactionInfo` при вызове метода `begin`, а каждый репозиторий при выполнении запроса
   будет обращаться к `TransactionInfo` за получением id транзакции.
2. Использовать контекст с полем `data`. Для этого создать интерфейс `DynamicContext`, который будет содержать метод
   `getData()`. Также необходимо указать, что `CreateOrderContext` реализует данный интерфейс. Идея в том, что все
   методы репозиториев должны будут принимать `DynamicContext` в качестве дополнительного параметра, из которого они
   смогут получить доступ к полю `data`, в котором будет находиться id транзакции. Интерфейс `DynamicContext` нужен для
   того, чтобы была возможность переиспользовать репозитории в других местах, поэтому нельзя напрямую использовать
   тип `CreateOrderContext` в сигнатуре методов.
3. Отказаться от использования репозиториев в основной логике в изолируемом модуле (core) и перенести их в наблюдателя
   `CreateOrderObserverImpl`, как это было продемонстрировано в последнем разделе. Тогда нет никаких препятствий к тому,
   чтобы в сигнатуры методов каждого репозитория добавить параметр `long transactionId`, так как теперь эти методы
   никогда не будут использоваться в изолируемом модуле (core). Тогда в самом наблюдателе `CreateOrderObserverImpl` при
   вызове методов репозиториев появляется возможность передавать напрямую id транзакции.

## Подробное руководство по инверсии зависимостей. Часть 2. Application модуль

Я, aa0ndrey, приветствую вас и приглашаю продолжить обсуждение темы инверсии зависимостей. В рамках данной финальной
части будет рассмотрен application модуль. Будут определена его ответственность и будет рассмотрено то, как он
взаимодействует со всеми остальными модулями.

Примеры, как и раньше, приведены на языке программирования java, но используются исключительно простые конструкции,
чтобы любой читатель, понимающий на самом базовом уровне синтаксис java, смог понять данную статью.

Если вы не читали первую часть, то 7, 8 и 9 раздел могут быть недостаточно понятны. В этом случае можно начать читать
данную статью, сразу перейдя к 10 разделу. Тогда в качестве краткого содержания всех пропущенных разделов, включая
разделы из первой части, можно считать то, что там были описаны подходы, позволяющие достаточно надежно изолировать core
модуль от инфраструктурных зависимостей с помощью шаблона наблюдатель, шаблона декоратор и контекста.

[Ссылка](https://habr.com/ru/post/582588/) на первую часть.

### 7. Проблема взаимосвязанных наблюдателей

В текущем и во всех последующих разделах рассматриваемые примеры, чтобы их не усложнять, сильно упрощены особенно с
точки зрения технической реализации. В них остаются только те детали, которые важны для моделирования соответствующих
примеров.

Представим, что необходимо автоматизировать процесс, который позволяет создавать от пользователя заказы на покупку
товара. И в рамках этого процесса необходимо проверять баланс пользователя. Баланс должен быть больше, чем стоимость
товара. Также необходимо, чтобы все взаимодействия с базой данных postgres выполнялись под единой транзакцией, а в
случае ошибки должен происходить откат транзакции.

Приведем одно из возможных решений с помощью шаблона наблюдатель, использование которого обсуждалось в предыдущей части.
Для этого для начала рассмотрим классы-данных, участвующие в процессе.

`CreateOrderRequest` - класс-данных запроса, который отправляет пользователь для создания заказа на товар.

```java
package aa0ndrey.dependency_inversion_guide.step_7.core.order;

public class CreateOrderRequest {
    private UUID userId;
    private UUID productId;
}
```

`User` - класс-данных пользователя.

```java
package aa0ndrey.dependency_inversion_guide.step_7.core.user;

public class User {
    private UUID id;
    private String name;
    private int balance;
}
```

`Product` - класс-данных товара.

```java
package aa0ndrey.dependency_inversion_guide.step_7.core.product;

public class Product {
    private UUID id;
    private String name;
    private int price;
}
```

`Order` - класс-данных заказа.

```java
package aa0ndrey.dependency_inversion_guide.step_7.core.order;

public class Order {
    private UUID id;
    private UUID userId;
    private UUID productId;
}
```

Ниже представлены классы репозиториев, которые позволят сохранять и получать данные из postgres:

```java
package aa0ndrey.dependency_inversion_guide.step_7.postgres.user;

public class UserRepositoryImpl {
    public User find(UUID id) {
        //реализация select * from user where user.id = ?
    }
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_7.postgres.product;

public class ProductRepositoryImpl {
    public Product find(UUID id) {
        //реализация select * from product where product.id = ?
    }
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_7.postgres.order;

public class OrderRepositoryImpl {
    public void create(Order order) {
        //реализация insert into order (id, user_id, product_id) values (?, ?, ?)
    }
}
```

Ниже представлен класс `TransactionManagerImpl`, позволяющий управлять транзакциями

```java
package aa0ndrey.dependency_inversion_guide.step_7.postgres.transaction_manager;

public class TransactionManagerImpl {
    public void begin() {
        //реализация начала транзакции
    }

    public void commit() {
        //реализация фиксации транзакции
    }

    public void rollback() {
        //реализация отката транзакции
    }

    public boolean isActive() {
        //реализация, позволяющая определить, что есть активная транзакция
    }
}
```

Стоит отметить, что в TransactionManagerImpl методы `begin` и `commit` теперь не содержат id транзакции, и в данном
случае нет необходимости решать задачу передачи id между инфраструктурными вызовами, как это было сделано в разделах
предыдущей части.

Также отметим и то, что были добавлены методы `rollback` и `isActive`. Метод `rollback` позволяет откатить транзакцию, а
метод `isActive` позволяет узнать, есть ли активная транзакция, то есть такая транзакция, которую либо не зафиксировали,
либо не откатили.

Ниже представлен класс `OrderService`, который содержит основную логику, соответствующую автоматизируемому процессу.

```java
package aa0ndrey.dependency_inversion_guide.step_7.core.order;

public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final List<CreateOrderObserver> observers;

    public void create(CreateOrderRequest request) {
        try {
            observers.forEach(observer -> observer.onStart()); //(1)

            var user = userRepository.find(request.getUserId());
            var product = productRepository.find(request.getProductId());

            if (user.getBalance() < product.getPrice()) {
                throw new RuntimeException("Недостаточно средств");
            }

            var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
            orderRepository.create(order);

            observers.forEach(observer -> observer.onEnd()); //(2)
        } finally {
            observers.forEach(observer -> observer.onFinally()); //(3)
        }
    }
}
```

Как и в предыдущих разделах для того, чтобы в модуле core избежать использования `TransactionManagerImpl` или его
интерфейса `TransactionManager` , используется отправка событий с помощью шаблона наблюдатель в `(1)`, `(2)` и `(3)`.
Отметим также, что теперь была добавлена конструкция try-finally для того, чтобы была возможность отправить и обработать
события вне зависимости от возможных исключений, возникающих в основной логике в блоке try.

И в заключении ниже представлен интерфейс и реализация шаблона наблюдатель для процесса создания заказа.

```java
package aa0ndrey.dependency_inversion_guide.step_7.core.order;

public interface CreateOrderObserver {
    default void onStart() {
    }

    default void onEnd() {
    }

    default void onFinally() {
    }
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_7.postgres.order;

public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManagerImpl transactionManagerImpl;

    @Override
    public void onStart() {
        transactionManagerImpl.begin();
    }

    @Override
    public void onEnd() {
        transactionManagerImpl.commit();
    }

    @Override
    public void onFinally() {
        if (transactionManagerImpl.isActive()) {
            transactionManagerImpl.rollback();
        }
    }
}
```

Как и в предыдущих разделах в методе `onStart` происходит открытие транзакции. В методе `onEnd` происходит фиксация
транзакции. В свою очередь в новом методе `onFinally` содержится код, который проверяет наличие активной транзакции.
Если есть активная транзакция, это означает, что было выброшено исключение, и транзакция не была зафиксирована, поэтому
вызывается метод `rollback`, который откатывает транзакцию.

Ниже приведена файловая структура модулей core и postgres:

Файловая структура core модуля

```text
├── order
│   ├── CreateOrderObserver.java
│   ├── CreateOrderRequest.java
│   ├── Order.java
│   ├── OrderRepository.java
│   └── OrderService.java
├── product
│   ├── Product.java
│   └── ProductRepository.java
└── user
    ├── User.java
    └── UserRepository.java
```

Файловая структура postgres модуля

```text
├── order
│   ├── CreateOrderObserverImpl.java
│   └── OrderRepositoryImpl.java
├── product
│   └── ProductRepositoryImpl.java
├── transaction_manager
│   └── TransactionManagerImpl.java
└── user
    └── UserRepositoryImpl.java
```

На текущий момент в приведенном решении особых сложностей и проблем нет. Есть всего один наблюдатель в инфраструктурном
модуле (postgres), который берёт на себя задачу по управлению транзакциями.

Но представим теперь, что было принято решение собирать метрики для приложения. В качестве механизма для сбора метрик
было решено использовать [open-telemetry](https://opentelemetry.io/). И в частности в рамках задачи по сбору метрик было
также решено собирать информацию о времени выполнения метода `create`, принадлежащего классу `OrderService`.

При этом старт отсчета начала времени выполнения должен быть обязательно до начала транзакции, а момент окончания
отсчета времени должен быть обязательно после завершения транзакции, вне зависимости от того, как транзакция
завершилась:
успешно или неуспешно.

Поскольку open-telemetry относится к другой инфраструктурной технологии, для него создан отдельный одноименный
инфраструктурный модуль. Это также означает, что в open-telemetry модуль будет добавлен класс, реализующий интерфейс
`CreateOrderObserver`, аналогично тому, как это сделано в postgres модуле.

Можно заметить первую проблему. В классе `OrderService` есть список `observers`, который используется для отправки
событий наблюдателям в одном и том же порядке для каждого из методов `onStart`, `onEnd` и `onFinally`.

Но для метода `onStart` необходимо, чтобы сначала вызывался наблюдатель из open-telemetry модуля, а уже затем из
postgres модуля. В свою очередь для метода `onFinally` необходимо наоборот, чтобы сначала вызывался наблюдатель из
postgres модуля, а уже затем из open-telemetry модуля.

Учитывая описанную проблему выше, рассмотрим возможное решение для open-telemetry модуля. Для этого для начала определим
класс, с помощью которого будет регистрироваться информация о промежутках времени выполнения. Пусть он задается
следующим образом:

```java
package aa0ndrey.dependency_inversion_guide.step_7.open_telemetry.time_span_manager;

public class TimeSpanManagerImpl {
    public void startTimeSpan(String name) {
        //реализация старта временного отрезка
    }

    public boolean isActive() {
        //реализация, определяющая, что есть промежуток времени, для которого ведется отсчет времени
    }

    public void stopTimeSpan() {
        //реализация завершения временного отрезка
    }
}
```

Метод `startTimeSpan` позволяет начать отсчет отрезка времени в момент вызова метода, при этом название временного
отрезка будет соответствовать параметру `name`.

Метод `stopTimeSpan` останавливает отсчет отрезка времени в момент вызова и отправляет информацию о временном отрезке в
соответствующую систему учета метрик.

Метод `isActive` определяет, что есть промежуток времени, для которого ведется отсчет времени.

Отметим, что класс `TimeSpanManagerImpl` сделан искусственно и сделан намерено сильно похожим
на `TransactionManagerImpl` для упрощения примеров. В действительности может оказаться, что класс `TimeSpanManagerImpl`
с его методами невозможно создать, используя open-telemetry. Но на суть рассматриваемых примеров это не влияет.

Ниже рассмотрим реализацию для интерфейса `CreateOrderObserver`, которая будет находиться в open-telemetry модуле.

```java
package aa0ndrey.dependency_inversion_guide.step_7.open_telemetry.order;

public class CreateOrderObserverImpl {
    public static class OnStart implements CreateOrderObserver { //(4)
        private final TimeSpanManagerImpl timeSpanManager;

        @Override
        public void onStart() {
            timeSpanManager.startTimeSpan("Создание заказа");
        }
    }

    public static class OnFinally implements CreateOrderObserver { //(5)
        private final TimeSpanManagerImpl timeSpanManager;

        @Override
        public void onFinally() {
            if (timeSpanManager.isActive()) {
                timeSpanManager.stopTimeSpan();
            }
        }
    }
}
```

Отметим, что для open-telemetry модуля для решения проблемы, связанной с очередностью вызова наблюдателей, был создан не
один класс, а два: `CreateOrderObserverImpl.OnStart` и `CreateOrderObserverImpl.OnFinally`, каждый из которых реализует
только по одному соответствующему методу из интерфейса `CreateOrderObserver`. Остальные нереализованные методы для этих
классов остаются со стандартной реализацией с пустым телом метода.

Идея данного решения заключается в том, чтобы добавить список наблюдателей в объект класса `OrderService`
в следующим порядке:

1. Объект класса `CreateOrderObserverImpl.OnStart` из open-telemetry модуля
2. Объект класса `CreateOrderObserverImpl` из postgres модуля
3. Объект класса `CreateOrderObserverImpl.OnFinally` из open-telemetry модуля

Поскольку классы `CreateOrderObserverImpl.OnStart` и  `CreateOrderObserverImpl.OnFinally` из open-telemetry модуля имеют
ровно по одному реализованному методу, то фактически для события `onStart` будет вызываться вначале наблюдатель из
open-telemetry модуля, а за ним наблюдатель из postgres модуля, а для события `onFinally` наоборот.

И тут можно заметить вторую проблему. В `CreateOrderObserverImpl` из postgres модуля внутри обработки
события `onFinally` при выполнении `transactionManager.rollback()` может возникнуть исключение, которое не позволит
выполнить обработку события в классе `CreateOrderObserverImpl.OnFinally` из open-telemetry модуля.

Это будет означать, что останется подвисший временной отрезок и информация о времени выполнения метода не будет учтена,
что является нежелательным поведением.

Чтобы решить эту проблему, можно, например, затребовать, чтобы наблюдатели принимали в качестве дополнительного
параметра исключение. Тогда, если в наблюдателе возникнет исключение, то оно будет передаваться следующему наблюдателю
вместе с событием вместо того, чтобы прервать процесс обработки.

Это возможно сделать, но решение станет значительно сложнее, чем хотелось бы. На текущий момент уже логика по обработке
событий расположена в нескольких местах: в postgres и open-telemetry модулях, что несколько осложняет понимание.

А из-за зависимостей, которые есть между наблюдателями, приходится дублировать наблюдателей и организовывать специальный
порядок их вставки в класс `OrderService`. Возникшая проблема с исключениями добавляет еще больше сложностей, которых
хотелось бы избежать.

Важным, что необходимо отметить в рамках данного раздела, является то, что решение со списком наблюдателей становится
значительно сложнее, когда существуют зависимости между наблюдателями. Как улучшить решение с наблюдателями, будет
рассмотрено в следующем разделе.

### 8. Промежуточный модуль application

Проблема из предыдущего раздела возникает из-за того, что используется список из наблюдателей, которые имеют между собой
зависимости, но при этом их выполнение происходит независимо друг от друга. Но что если добавить возможность для
совместного выполнения между зависимыми наблюдателями, при этом не добавляя инфраструктурные интерфейсы в модуль core?
Для этого понадобится дополнительный промежуточный модуль application.

Идея заключается в том, чтобы разрешить в application модуле использовать даже инфраструктурные интерфейсы, подобные
`TransactionManager` и `TimeSpanManager`.

Важно отметить, что это не означает добавления прямой зависимости на инфраструктурные модули, такие как postgres или
open-telemetry. В application модуле будет все также использоваться инверсия зависимостей с помощью интерфейсов, но в
отличие от core модуля в нем будет возможно использовать и инфраструктурные интерфейсы.

Ниже приведена диаграмма модулей

```text
         ┌─────────────┐
      ┌─►│    Core     │◄─┐
      │  └──────▲──────┘  │
      │         │         │
      │  ┌──────┴──────┐  │
      ├─►│ Application │◄─┤
      │  └─────────────┘  │
┌─────┴───────┐ ┌─────────┴───┐
│     OTel    │ │   Postgres  │
└─────────────┘ └─────────────┘
```

OTel - сокращение для open-telemetry, используемое в официальной документации.

И как ранее предлагалось, в application модуль будут добавлены интерфейсы для `TransactionManager` и `TimeSpanManager`,
реализации которых соответственно находятся в postgres и open-telemetry модуле.

```java
package aa0ndrey.dependency_inversion_guide.step_8.application.transaction_manager;

public interface TransactionManager {
    void begin();

    void commit();

    void rollback();

    boolean isActive();
}
```

```java
package aa0ndrey.dependency_inversion_guide.step_8.application.time_span_manager;

public interface TimeSpanManager {
    void startTimeSpan(String name);

    boolean isActive();

    void stopTimeSpan();
}
```

И теперь вместо того, чтобы использовать несколько отдельных зависимых между собой наблюдателей, можно создать всего
лишь одного наблюдателя в application модуле, организовав явное взаимодействие между `TransactionManager` и
`TimeSpanManager`. При этом наблюдатели в postgres и open-telemetry модуле должны быть удалены.

```java
package aa0ndrey.dependency_inversion_guide.step_8.application.order;

public class CreateOrderObserverImpl implements CreateOrderObserver {
    private final TransactionManager transactionManager;
    private final TimeSpanManager timeSpanManager;

    @Override
    public void onStart() {
        timeSpanManager.startTimeSpan("Создание заказа");
        transactionManager.begin();
    }

    @Override
    public void onEnd() {
        transactionManager.commit();
    }

    @Override
    public void onFinally() {
        try { //(1)
            if (transactionManager.isActive()) {
                transactionManager.rollback(); //(2)
            }
        } finally { //(3)
            if (timeSpanManager.isActive()) {
                timeSpanManager.stopTimeSpan();
            }
        }
    }
}
```

В методе `onStart` начинается отсчет временного отрезка, и затем начинается транзакция. В методе `onEnd` происходит
фиксация транзакция. А самое интересное происходит в методе `onFinally`. В нем транзакция откатывается, если она
активна, и затем останавливается отсчет временного отрезка.

Как подмечалось в предыдущем разделе, метод `rollback` в `(2)` также может выбросить исключение, что потенциально могло
бы помешать окончанию отсчета временного отрезка с помощью `TimeSpanManager`. Но так как используется конструкция
try-finally в `(1)` и `(3)`, то выполнение приложения в любом случае дойдет до вызова метода `stopTimeSpan`, если,
конечно, не произойдет критических проблем с самим приложением.

Поскольку с помощью промежуточного модуля `application` можно всегда использовать всего лишь одного наблюдателя, то
в `OrderService` код также можно упростить.

```java
package aa0ndrey.dependency_inversion_guide.step_8.core.order;

public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CreateOrderObserver observer; //(4)

    public void create(CreateOrderContext context) {
        try {
            observer.onStart(context); //(5)
            var request = context.getRequest();

            var user = userRepository.find(request.getUserId());
            var product = productRepository.find(request.getProductId());

            if (user.getBalance() < product.getPrice()) {
                throw new RuntimeException("Недостаточно средств");
            }

            var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
            orderRepository.create(order);

            observer.onEnd(context); //(6)
        } finally {
            observer.onFinally(context); //(7)
        }
    }
}
```

Теперь в классе `OrderService` используется всего лишь один наблюдатель. Это можно заметить в `(4)`, `(5)`, `(6)`,
`(7)`.

После проведенных изменений файловая структура проекта будет следующей.

Файловая структура core модуля

```text
├── order
│   ├── CreateOrderObserver.java
│   ├── CreateOrderRequest.java
│   ├── Order.java
│   ├── OrderRepository.java
│   └── OrderService.java
├── product
│   ├── Product.java
│   └── ProductRepository.java
└── user
    ├── User.java
    └── UserRepository.java
```

Файловая структура application модуля

```text
├── order
│   └── CreateOrderObserverImpl.java
├── time_span_manager
│   └── TimeSpanManager.java
└── transaction_manager
    └── TransactionManager.java
```

Файловая структура open-telemetry модуля

```text
└── time_span_manager
    └── TimeSpanManagerImpl.java
```

Файловая структура postgres модуля

```text
├── order
│   └── OrderRepositoryImpl.java
├── product
│   └── ProductRepositoryImpl.java
├── transaction_manager
│   └── TransactionManagerImpl.java
└── user
    └── UserRepositoryImpl.java
```

Еще раз отметим, что теперь нет наблюдателей в postgres и open-telemetry модулях. А интерфейсы `TransactionManager`
и `TimeSpanManager` были добавлены в application модуль в единственного наблюдателя.

За счет добавления промежуточного модуля application, получилось организовать и сконцентрировать взаимодействие между
несколькими инфраструктурными модулями в одном единственном наблюдателе в application модуле. При этом инфраструктурные
интерфейсы не были добавлены в изолируемый модуль core. Также данное решение избавляет от необходимости реализовывать
общий сложный механизм по передаче управления между независимыми наблюдателями.

### 9. Использование шаблона декоратор

Довольно часто инфраструктурный вспомогательный код, например, связанный с управлением транзакциями, может или должен
быть размещен в начале и в конце выполнения основной логики. Эта закономерность очень удобна. В таких случаях, вместо
того, чтобы использовать шаблон наблюдатель, можно использовать шаблон декоратор.

Тогда код, связанный с шаблоном наблюдатель, можно убрать. И в этом случае класс `OrderService` станет таким, как
указано ниже.

```java
package aa0ndrey.dependency_inversion_guide.step_9.core.order;

public class OrderCoreService implements OrderService { //(1)
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    public void create(CreateOrderRequest request) {
        var user = userRepository.find(request.getUserId());
        var product = productRepository.find(request.getProductId());

        if (user.getBalance() < product.getPrice()) {
            throw new RuntimeException("Недостаточно средств");
        }

        var order = new Order(UUID.randomUUID(), user.getId(), product.getId());
        orderRepository.create(order);
    }
}
```

Стоит отметить, что класс `OrderCoreService` стал похож на тот, что был представлен в самом начале, в первой части в
разделе под номером 1. Отличие заключается только в том, что в `(1)` класс был переименован в `OrderCoreService`, а его
предыдущее название перешло к интерфейсу `OrderService`, который он реализует.

Ниже представлен интерфейс.

```java
package aa0ndrey.dependency_inversion_guide.step_9.core.order;

public interface OrderService {
    void create(CreateOrderRequest request);
}
```

Интерфейс нужен для того, чтобы скрыть от внешних клиентов (пользователей) класса знание о том, что сервис из core
модуля может быть обернут сервисом из application модуля.

Это нужно для того, чтобы все клиенты класса в качестве зависимости указывали интерфейс сервиса из core модуля, а не
конкретную реализацию внешнего сервиса-декоратора из application модуля. Пример, дополнительно раскрывающий назначение
интерфейса, будет приведен в 12 разделе.

Далее рассмотрим класс декоратора. Он будет размещен в модуле application вместо `CreateOrderObserverImpl`.

```java
package aa0ndrey.dependency_inversion_guide.step_9.application.order;

public class OrderAppService implements OrderService {
    private final OrderService coreService;
    private final TransactionManager transactionManager;
    private final TimeSpanManager timeSpanManager;

    @Override
    public void create(CreateOrderRequest request) {
        try {
            timeSpanManager.startTimeSpan("Создание заказа"); //(2)
            transactionManager.begin(); //(3)

            coreService.create(request); //(4)

            transactionManager.commit(); //(5)
        } finally {
            try {
                if (transactionManager.isActive()) {
                    transactionManager.rollback(); //(6)
                }
            } finally {
                if (timeSpanManager.isActive()) {
                    timeSpanManager.stopTimeSpan(); //(7)
                }
            }
        }
    }
}
```

`OrderAppService` содержит в себе всю необходимую вспомогательную инфраструктурную логику, связанную с управлением
транзакциями и с отсчетом временного отрезка, что ранее было размещено в наблюдателе. Это можно отметить в  `(2)`,
`(3)`, `(5)`, `(6)`, `(7)`. Также в середине метода в `(4)` между инфраструктурной логикой находится вызов метода с
основной логикой из класса `OrderCoreService`, расположенного в модуле core.

Вместе с шаблоном декоратор аналогично шаблону наблюдатель можно использовать контекст, чтобы, например, какие-то
инфраструктурные данные, которые были получены на уровне декоратора, передать в наблюдателей или наоборот. Про
использование контекста было в предыдущей части в разделах с 4 по 6.

Если читатель знаком с технологиями, такими как: Spring, JavaEE и другими, которые содержат какую-либо разновидность
аннотации `@Transactional`, то данный класс `OrderAppService` отлично подходит для использования данной аннотации. Но
чтобы сохранять примеры максимально нейтральными к используемым технологиям и языкам программирования, здесь и далее
будет продолжено явное использование класса `TransactionManagerImpl` и интерфейса `TransactionManager`.

По этой же причине в рамках данной статьи не будет рассмотрено использование аспектно-ориентированного программирования,
которому в частности можно отнести аннотацию `@Transactional`.

Для читателей, которые уже знакомы с аспектно-ориентированным программированием (АОП), по крайней мере, по примеру
библиотеки AspectJ в Java, стоит отметить только то, что АОП по своей сути позволяет более гибко и неявно создавать
декораторы. Поэтому всюду, где идет речь об использовании шаблона декоратор, можно применять АОП.

Может возникнуть вопрос: зачем использовать шаблон наблюдатель, если декоратор решает ту же самую задачу, но проще?

Шаблон наблюдатель является более универсальным инструментом, по сравнению со стандартным подходом к использованию
шаблона декоратор, так как в рамках использования шаблона наблюдатель можно добавлять точки расширения в произвольном
месте, находясь на любой глубине вложенности внутри метода. В свою очередь, шаблон декоратор позволяет добавлять
расширение только перед и после декорируемого метода.

### 10. Ответственность application модуля

В предыдущих разделах было предложено использовать application модуль для того, чтобы упростить использование и
изолирование инфраструктурных модулей, обеспечивающих работу core модуля. Но при этом множество вопросов и тем,
касающихся application модуля, не были рассмотрены. Хотя здесь их достаточно.

Можно начать с определения задачи, которую неудобно или невозможно решать исключительно в core и инфраструктурных
модулях, при этом, не смешивая ответственность и не добавляя лишних зависимостей.

Как было продемонстрировано ранее, при построении архитектуры приложения есть потребность в организации межмодульного
взаимодействия для выполнения общих задач. Поэтому существует потребность в модуле, который позволит явно координировать
работу нескольких слабо связанных модулей. Пусть для этих задач будет использован application модуль.

Отсюда следует, что те задачи, которые возможно комфортно решить исключительно в одном модуле в соответствии с его
предназначением, должны быть решены в нем, не вынося их в application модуль. Иначе ответственность других модулей
начнет размываться в сторону application модуля.

Так, например, даже если в application модуле используются механизмы транзакций из postgres модуля, то крайне
нежелательно, если помимо управления транзакциями в application модуле будет использован тот или иной механизм для
построения и отправки SQL-запросов.

То есть, с одной стороны, application модуль должен уметь координировать работу нескольких модулей, с другой стороны, он
не должен брать и использовать больше положенного. А это означает, что важно как-то управлять и декларировать
возможности и классы, используемые в application модуле из других модулей.

Также тут важно ответить на следующий вопрос: должны ли классы, используемые в application модуле из инфраструктурных
модулей, быть спрятаны за стабильными абстракциями, устойчивыми к изменениям? Проще говоря, нужно ли создавать такие
интерфейсы в application модуле, которые не потребуется менять в случае изменений инфраструктурного модуля.

С одной стороны, с точки зрения core модуля не имеет значения, использует ли application модуль устойчивые к изменениям
интерфейсы или application модуль использует прямые реализации из инфраструктурного модуля. В любом случае основная
логика в core модуле будет заизолирована от инфраструктурных зависимостей и изменений. А тогда зачем платить дважды?

С другой стороны, если для создания общих интерфейсов в коде application модуля и для их реализации в соответствующих
инфраструктурных модулях не придется прилагать слишком уж много усилий, то лучше, конечно, создавать такие интерфейсы,
которые способны добавить устойчивости к изменениям даже в application модуль.

Важно подчеркнуть, что не любые интерфейсы являются устойчивыми к изменениям и добавление интерфейса еще не означает
увеличение устойчивости.

В предыдущих частях, когда было предложено использовать модуль application, направление зависимостей между application
модулем и инфраструктурными модулями было просто задано без рассмотрения каких-либо альтернатив. А учитывая, что нет
явных причин отказываться от использования конкретной реализации, получается, что тут тоже есть о чем подумать.

Таким образом, можно выделить следующие вопросы, на которые необходимо ответить при выборе подхода к реализации
application модуля:

1. С помощью чего в application модуле предоставляются гарантии того, что в нем не используется больше инфраструктурных
   классов и возможностей, чем это необходимо, для координации работы нескольких модулей?
2. Нужно ли в application модуле стремиться использовать интерфейсы, устойчивые к изменениям инфраструктурных модулей? И
   в каких случаях это делать?
3. В какую сторону должна быть направлена зависимость: от application модуля к инфраструктурному модулю или наоборот?

### 11. Зависимость от инфраструктурных модулей в сторону application модуля

Рассмотрим следующую диаграмму, на которой представлено несколько модулей.

```text
  ┌──────────────┐
┌►│     Core     │◄─────────┬───────────────┐
│ └──────▲───────┘          │               │
│        │                  │               │
│ ┌──────┴───────┐          │               │
│ │  Application │◄──────┬──┼────────────┐  │
│ └──────▲───────┘       │  │            │  │
│        │               │  │            │  │
│ ┌──────┴───────┐ ┌─────┴──┴─────┐ ┌────┴──┴──────┐
└─┤  Postgres    │ │    OTel      │ │    REST      │
  └──────────────┘ └──────────────┘ └──────────────┘
```

- Есть модуль core, и от него зависят все остальные модули.
- Есть модуль application, и от него зависят все инфраструктурные модули.
- И есть три инфраструктурных модуля: OTel (open-telemetry), Postgres и REST

Здесь был добавлен REST модуль. Предполагается, что этот модуль содержит классы, позволяющие приложению получать HTTP
запросы в соответствии с REST подходом.

REST модуль был добавлен для того, чтобы в примерах учитывать не только инфраструктурные модули, методы которых
преимущественно вызываются из application модуля, но и инфраструтурные модули, из которых преимущественно вызываются
методы application модуля.

Например, в REST модуле может быть `OrderRestController`, который получает HTTP запросы, осуществляет какую-либо
конвертацию данных и затем вызывает метод `create` из `OrderAppService` из application модуля.

Отметим, что все зависимости от инфраструктурных модулей направлены в сторону application модуля. Для этого классы из
инфраструктурного модуля должны реализовывать интерфейсы из application модуля. Например, так было сделано для
интерфейсов `TimeSpanManager` и `TransactionManager` и классов `TimeSpanManagerImpl` и `TransactionManagerImpl`.

Но насколько просто создавать пары интерфейс-реализация? Все зависит от того, насколько устойчивым к изменениям
требуется разработать интерфейс.

Интерфейс, который не пытается скрыть инфраструктурные особенности и не пытается кардинальным образом изменить суть
вызываемых методов, будет в рамках данной статьи называться _общим инфраструктурным интерфейсом_. При этом
подразумевается, что такой интерфейс создается с идей обобщить использование нескольких взаимозаменяемых и близких
технологий.

Например, использование шаблона наблюдатель с соответствующим интерфейсом для того, чтобы скрыть
использование `TransactionManagerImpl`, не является примером общего инфраструктурного интерфейса, так как шаблон
наблюдатель скрывает используемую технологию и кардинальным образом меняет суть вызываемых методов.

То есть под созданием общего инфраструктурного интерфейса подразумевается создания такого интерфейса, который, например,
для задачи управления транзакциями предложит интерфейс `TransactionManager`, который будет подходить более чем под одну
конкретную систему управления базами данных.

Есть множество примеров того, как принимались и принимаются попытки создания интерфейсов для обобщения использования
технологий. Но у таких обобщений могут быть недостатки.

Использование общего инфраструктурного интерфейса не может дать больше производительности и потреблять меньше ресурсов,
чем использование конкретной реализации, спрятанной за интерфейсом. Это следует из того, что общий интерфейс может
использовать конкретную реализацию неоптимальным образом для определенного рода задач.

Также общий инфраструктурный интерфейс не может дать больше возможностей, чем конкретная реализация. В общем
инфраструктурном интерфейсе не может быть того, чего невозможно сделать с помощью конкретной реализации, а вот обратное
верно.

Но помимо того, что общий инфраструктурный интерфейс может быть менее эффективен и может давать меньше возможностей,
сама по себе задача создания общего инфраструктурного интерфейса может быть не простой, а иногда и невозможной. Для того
чтобы создать общий инфраструктурный интерфейс, необходимо изучить существующие решения, разобраться в том, как их
возможно обобщить, а также, возможно, спрогнозировать их развитие.

А самое главное, что все эти недостатки общих инфраструктурных интерфейсов и затраты труда для их разработки и поддержки
не имеют особого смысла с точки зрения защиты core модуля от влияния инфраструктурных зависимостей.

Сore модуль, как было продемонстрировано ранее, защищен подходами и шаблонами проектирования, которые не пытаются
обобщить использование инфраструктурных технологий. И как было сказано ранее, для core модуля безразлично, использует ли
application модуль конкретные реализации или использует интерфейсы для инфраструктурных технологий.

Стоит отметить, что тут нет попытки раскритиковать подходы к созданию общих инфраструктурных интерфейсов для различных
технологий в глобальном смысле. Здесь лишь делается акцент на том, что у этого есть свои недостатки, которые в
определенных случаях могут перевешивать преимущества конкретно в контексте разговора про application модуль.

Тогда допустим, что при разработке application модуля не предпринимается попыток создания общих инфраструктурных
интерфейсов, если это принесет только больше проблем. Но поскольку зависимости направлены от инфраструктурных модулей в
сторону application модуля, то интерфейсы все равно необходимо создавать.

В этом случае их можно создавать точь-в-точь похожими на реализацию. И это будут именно те интерфейсы, которые не
добавляет для application модуля устойчивости к изменениям в инфраструктурных модулях, но, как было сказано ранее, это и
не нужно.

Тогда какую задачу решают такие интерфейсы в application модуле?

Как было отмечено в предыдущем разделе, для application модуля важно иметь возможность ограничить классы и механизмы,
используемые из инфраструктурных модулей. То есть, например, важно запретить возможность собирать и отправлять
SQL-запросы прямо из application модуля.

В этом смысле использование инверсии зависимостей с интерфейсами позволяет декларировать инфраструктурные возможности,
которые есть в application модуле. То есть, если в application модуле есть только интерфейс `TransactionManager`, то из
application модуля невозможно собирать и отправлять SQL-запросы до тех пор, пока не будет добавлен соответствующий
интерфейс.

Но тут появляется другая проблема. Предположим, что было принято решение создавать интерфейсы, которые точь-в-точь
повторяют классы и интерфейсы из open-telemetry модуля для того, чтобы использовать их в application модуле. Ниже будет
представлено такое решение для демонстрационных целей, несмотря на то, что для open-telemetry, вероятно, можно подобрать
более подходящие интерфейсы.

Пусть мы начнем с класса SpanBuilder, который есть в open-telemetry.

```java
package io.opentelemetry.api.trace;

public interface SpanBuilder {
    //...

    SpanBuilder addLink(SpanContext spanContext);

    SpanBuilder setParent(Context context);

    Span startSpan();
}
```

Предназначение классов и методов не играет особой роли для примера, поэтому для них не будет представлено описания.

Предположим, что был добавлен интерфейс для `SpanBuilder` в application модуль. Но чтобы его использовать, необходимо
также добавить интерфейсы для `SpanContext`, `Context` и `Span`, так как они присутствуют в сигнатурах методов. Но после
того, как будут добавлены интерфейсы для `SpanContext`, `Context` и `Span`, внутри уже их сигнатур методов могут быть
другие классы, для которых необходимо также добавить интерфейсы.

Проблема тут заключается в том, что граф транзитивных зависимостей может быть достаточно большим, даже несмотря на то,
что будут добавляться только те сигнатуры методов, которые необходимы для application модуля. Более того, помимо
интерфейсов, добавляемых в application модуль, на стороне open-telemetry модуля необходимо создавать реализации, которые
будут делегировать вызовы классам из библиотеки open-telemetry.

С одной стороны, добавление таких интерфейсов дает возможность декларировать используемые классы в application модуле с
точностью до сигнатуры метода. С другой стороны, хотелось бы избежать бессмысленного копирования сигнатур и
делегирования вызовов.

Как уже стало понятно, иногда задача добавления интерфейса для использования классов из инфраструктурных модулей в
application модуле, может быть непростой. Но возможна и иная ситуация.

Например, создать интерфейс может быть достаточно просто, потому что может не быть длинных транзитивных зависимостей.
Также может так получится, что технологии легко подвергаются обобщению. Более того, могут существовать уже готовые
решения, предлагающие общие инфраструктурные интерфейсы.

Например, для управления транзакциями в java в технологии spring существует
отдельная [библиотека](https://mvnrepository.com/artifact/org.springframework/spring-tx), которую можно подключать
независимо от библиотек с базами данных. В этом случае использование уже готовой библиотеки с общими инфраструктурными
интерфейсами может быть очень удобно.

По вопросу создания интерфейсов в рамках данного раздела было уже сказано многое. Но это не единственное, на что влияет
направление зависимостей от инфраструктурных модулей в сторону application модуля.

За счет того, что все зависимости направлены в сторону application модуля, application модуль можно использовать, как "
площадку" для размещения интерфейсов для классов из инфраструктурных модулей и для размещения классов-данных, чтобы
использовать их не в application модуле, а между инфраструктурными модулями.

Например, если потребуется в postgres модуле замерить время выполнения определенных запросов с помощью open-telemetry,
то нет необходимости добавлять явную зависимость между postgres и open-telemetry модулем. В этом случае можно в postgres
модуле использовать интерфейс `TimeSpanManager` из application модуля.

На самом деле это не всегда самое оптимальное решение. Так как зависимость все равно есть, просто она проходит через
application модуль. О том, как это можно сделать иначе, описано в 14 разделе.

Также то, что направление зависимостей всех инфраструктурных модулей направлено в сторону application модуля, удобно
тем, что application модуль можно использовать для размещения общих утилитных классов, которые невозможно разместить в
core модуле.

Подведем итог для данного раздела. При организации зависимостей от всех инфраструктурных модулей в сторону application
модуля можно отметить следующее:

1. Нет сильной необходимости изолировать application модуль от инфраструктурных зависимостей.
2. Использование общих инфраструктурных интерфейсов может быть менее эффективно, чем конкретная реализация.
3. Общие инфраструктурные интерфейсы могут не предоставлять всех возможностей, которые есть в конкретных реализациях.
4. Создание общих инфраструктурных интерфейсов может быть непростой задачей.
5. Создание интерфейсов, которые точь-в-точь повторяют конкретную реализацию, может приводить к большому количеству
   бессмысленного (boilerplate) кода.
6. Использование интерфейсов позволяет явно декларировать возможности application модуля относительно использования
   классов из инфраструктурных модулей.
7. Общие инфраструктурные интерфейсы могут быть уже реализованы в сторонних библиотеках, что значительно упрощает
   задачу.
8. За счет того, что все инфраструктурные модули зависят от application модуля, то application модуль можно
   использовать, как "площадку" для размещения инфраструктурных интерфейсов, классов-данных и общих утилитных классов,
   чтобы их использовать между инфраструктурными модулями.

### 12. Зависимость от application модуля в сторону инфраструктурных модулей

Рассмотрим следующую диаграмму, на которой представлено несколько модулей.

```text
  ┌──────────────┐
┌►│     Core     │◄─────────┬───────────────┐
│ └──────▲───────┘          │               │
│        │                  │               │
│ ┌──────┴───────┐          │               │
│ │  Application ├───────┬──┼────────────┐  │
│ └──────┬───────┘       │  │            │  │
│        │               │  │            │  │
│ ┌──────▼───────┐ ┌─────▼──┴─────┐ ┌────▼──┴──────┐
└─┤  Postgres    │ │    OTel      │ │    REST      │
  └──────────────┘ └──────────────┘ └──────────────┘
```

Данная диаграмма отличается от диаграммы из предыдущего раздела только тем, что зависимости от application модуля
направлены в сторону инфраструктурных модулей.

На что это влияет? Во-первых, отметим, что при таком направлении зависимостей в application модуле нет необходимости
создавать и использовать интерфейсы. В таком случае в application модуле можно использовать все возможности,
предоставляемые конкретной реализацией той или иной технологии.

С одной стороны, это хорошо в том плане, что технологию можно использовать самым эффективным образом и при этом нет
необходимости создавать интерфейсы, повторяющие точь-в-точь классы из инфраструктурных модулей. Также нет необходимости
думать о том, как скрыть технологии под общий устойчивый к изменениям интерфейс, так как это и не нужно.

С другой стороны, в данном примере пока не определено, каким образом ограничиваются возможности application модуля по
использованию инфраструктурных классов, чтобы, например, не дать возможность собирать и отправлять SQL-запросы прямо из
application модуля. В предыдущем разделе, эту обязанность брали на себя именно интерфейсы.

Для решения задачи по ограничению application модуля использовать те или иные инфраструктурные возможности необязательно
применять интерфейсы, которые являются частью языка программирования. Для этих целей существуют определенные технологии,
позволяющие декларировать, какие классы являются доступными для использования вне модуля.

Например, в java в частности для этих целей в 9 версии был добавлен механизм под
названием [Jigsaw](https://openjdk.java.net/projects/jigsaw/). Но, к сожалению, он был добавлен достаточно поздно, и
поэтому практически никто им не пользуется, что из-за особенностей реализации осложняет его использование и в других
проектах. Но в качестве живой альтернативы для этих целей в java можно использовать
библиотеку [ArchUnit](https://www.archunit.org/).

Задача по декларированию классов, которые доступны для использования вне модуля, не является чем-то особенным. Поэтому,
вероятно, в актуальных языках программирования и в сопутствующих им технологиям, найдутся те или иные механизмы,
позволяющие решить эту задачу. Если в вашем стеке технологий нет подобного механизма, возможно, вы можете стать первым,
кто его напишет.

Обратимся к следующей особенности. В предыдущем примере не вызывало вопросов, как из REST модуля управление перейдет в
application модуль, так как REST модуль зависел от application модуля.

Сейчас же зависимость направлена в другую сторону. Но на самом деле код внутри REST модуля никак не поменяется, каким бы
не было направление зависимостей между REST модулем и application модулем. Это связано с тем, что внутри REST модуля
должны использоваться интерфейсы для сервисов из core модуля.

Поэтому не имеет значения, есть ли у сервиса из core модуля сервис-декоратор из application модуля, потому что в любом
случае в REST модуле будет использоваться интерфейс для сервиса, под которым потенциально может быть спрятан класс из
application модуля. Подробнее продемонстрировано в примере ниже.

```java
package aa0ndrey.dependency_inversion_guide.step_12.rest.order;

public class OrderRestController {
    private final OrderService orderService; //(1)

    public void create(HttpRequest httpRequest) {
        var createOrderRequest = extractCreateOrderRequest(httpRequest);
        orderService.create(createOrderRequest);
    }

    private CreateOrderRequest extractCreateOrderRequest(HttpRequest httpRequest) {
        //Получение CreateOrderRequest из HttpRequest
    }
}
```

За интерфейсом `OrderService` в `(1)` может находиться реализация `OrderAppService`, которая находится в application
модуле, несмотря на то, что зависимость направлена от application модуля в сторону REST модуля.

В предыдущем примере, application модуль брал на себя еще две дополнительные ответственности. Он являлся площадкой для
размещения общих инфраструктурных интерфейсов и классов-данных, а также мог потенциально хранить общие утилитные классы
для использования в инфраструктурных модулях.

Эти возможности, которые предоставлял application модуль, были полезными, но необязательными для application модуля.
Ранее для application модуля в качестве основной ответственности было определено, что он в первую очередь должен
заниматься координацией совместного выполнения нескольких модулей.

Поэтому те возможности можно просто перенести в отдельный модуль. Например, общий infrastructure модуль. Тогда диаграмма
модулей будет следующей.

```text
      ┌──────────────┐
┌───┌►│     Core     │◄─────────┬───────────────┐
│   │ └──────▲───────┘          │               │
│   │        │                  │               │
│   │ ┌──────┴───────┐          │               │
│ ┌─┼─┤  Application ├───────┬──┼────────────┐  │
│ │ │ └──────┬───────┘       │  │            │  │
│ │ │        │               │  │            │  │
│ │ │ ┌──────▼───────┐ ┌─────▼──┴─────┐ ┌────▼──┴──────┐
│ │ └─┤  Postgres    │ │    OTel      │ │    REST      │
│ │   └──────┬───────┘ └────────┬─────┘ └───────┬──────┘
│ │          │                  │               │
│ │   ┌──────▼───────┐          │               │
│ └──►│Infrastructure│◄─────────┴───────────────┘
│     └──────┬───────┘
└────────────┘
```

В итоге при направлении зависимостей от application модуля в сторону инфраструктурных модулей, удалось сразу избавиться
от нескольких проблем, которые были обозначены в предыдущем разделе, но были получены новые.

Самое главное, что данное решение позволяет сразу использовать реализации для конкретных технологий, что, с одной
стороны, является преимуществом, с другой стороны, недостатком. О том, какой все-таки подход выбрать, описано в
следующем разделе.

### 13. Выбор направления зависимостей между application и инфраструктурными модулями

Возможно ли в каждом отдельном случае использовать наиболее подходящее направление зависимости между application и
инфраструктурным модулем? Да, возможно.

Например, если для используемых механизмов из инфраструктурного модуля можно легко разработать общий инфраструктурный
интерфейс или он уже кем-то разработан, то, конечно, лучше направить зависимость от инфраструктурного модуля в сторону
application модуля.

Если без огромных затрат усилий и каких-либо других недостатков можно добиться дополнительной устойчивости к изменениям
в application модуле, то почему бы этого не сделать?

Но в случае, если с разработкой интерфейса могут возникнуть какие-то сложности, или в случае, если общий
инфраструктурный интерфейс может не давать тех возможностей, которые нужны, то лучше, конечно, использовать зависимость,
направленную от application модуля в сторону инфраструктурного модуля.

Также возможна ситуация, когда в начале было решено использовать общий инфраструктурный интерфейс, а после стало ясно,
что, например, не хватает каких-либо возможностей из конкретной реализации. В этом случае стоит поменять направление
зависимостей.

Заметим, что изменить зависимость в уже разрабатываемой системе с направления от инфраструктурного модуля в сторону
application модуля на направление от application модуля в сторону инфраструктурного модуля обычно проще, чем это сделать
наоборот. Это связано с тем, что при любом направлении зависимостей, именно application модуль использует механизмы из
инфраструктурных модулей.

Исходя из вышесказанного, следует, что оптимальной стратегией является изначально рассматривать направление зависимостей
от инфраструктурных модулей в сторону application модуля. И только когда будет доказано, что данное направление чем-то
будет неудобно, следует его поменять. Это может случиться как сразу, еще до начала разработки на этапе проектирования,
так и после.

Рассмотрим пример, как могут развиваться события с направлением зависимостей между application модулем и
инфраструктурными модулями. Предположим, что есть java-разработчик, который использует spring в качестве основного
фреймворка. И он решил использовать redis для кэширования. Для этого он создал отдельный инфраструктурный одноименный
redis модуль.

При этом java-разработчик изучив spring решил из него использовать уже готовые
[общие инфраструктурные интерфейсы](https://docs.spring.io/spring-framework/docs/5.3.13/reference/html/integration.html#cache)
для кэширования, которые не привязаны к конкретной реализации. Поэтому зависимость была направлена от redis модуля в
сторону application модуля.

Затем java-разработчик решил использовать менеджер распределенных блокировок (distributed lock manager), при этом также
используя redis и
библиотеку [redisson](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers), добавив ее в
redis модуль.

Изучив подходы, алгоритмы и api, которые используются, например,
в [zookeeper](https://zookeeper.apache.org/doc/r3.1.2/recipes.html) для распределенных блокировок, и сравнив их с
[redlock](https://redis.io/topics/distlock) алгоритмом, который используется в redis, разработчик сделал вывод, что
создать общий инфраструктурный интерфейс для распределенных блокировок из zookeeper и redis ему будет достаточно тяжело,
если вообще возможно, так как, например, в redis для удержания блокировок учитывается время.

Тогда java-разработчик решил развернуть зависимость, чтобы она была направлена от application модуля в сторону redis
модуля для того, чтобы использовать напрямую блокировки из redis.

При этом он решил закрыть доступ до всех других классов, которые не относятся к механизмам блокировок и кэшированию, с
помощью настроек зависимостей для модулей. И раз проект написан на java, то для закрытия доступов он решил
использовать [ArchUnit](https://www.archunit.org/).

Вероятно, что история может развиваться и иначе. Разработчик может сделать достаточно успешную абстракцию, создав общие
инфраструктурные интерфейсы, для использования распределенных блокировок. Возможно, его общие инфраструктурные
интерфейсы будут предоставлять не все возможности, которые есть в redis, но они могут быть полезны в будущем для
перехода на zookeeper, если, например, нет уверенности в окончательном решении использовать redis.

Затем в случае, если возможности его общих инфраструктурных интерфейсов себя исчерпают, он сможет перейти на прямую
реализацию из redis.

Отметим, что именно за счет использования надежной и универсальной защиты core модуля от инфраструктурных зависимостей,
появляется возможность переключиться на использование конкретных реализаций из инфраструктурных модулей внутри
application модуля в случае, если общие инфраструктурные интерфейсы чем-то не подходят или их создание не оправдано.

Это происходит за счет того, что core модуль не пытается использовать или даже декларировать общие инфраструктурные
интерфейсы, а перекладывает их использование в application модуль за счет шаблона наблюдатель, шаблона декоратор и за
счет использования контекста.

Таким образом достигается беспроигрышная ситуация (win-win), когда, с одной стороны, есть сильные гарантии того, что
core модуль не будет подвержен влиянию зависимостей из инфраструктурных модулей, а с другой стороны, сохраняется
потенциал использовать все возможности конкретной реализации из инфраструктурного модуля внутри application модуля, если
это будет необходимо.

### 14. Изолирование инфраструктурных модулей

Иногда может возникнуть потребность настроить взаимодействие между инфраструктурными модулями. И что тогда в этом случае
делать?

Во-первых, можно явно определить зависимость между двумя инфраструктурными модулями. И тогда один модуль сможет
использовать другой.

Во-вторых, можно не определять явно зависимость между модулями и использовать интерфейсы, которые есть в модуле
application, в случае, если все зависимости направлены в сторону application модуля. Если часть зависимостей направлены
в сторону инфраструктурных модулей, то значит есть дополнительный infrastructure модуль, в котором возможна регистрация
инфраструктурных интерфейсов взамен application модуля.

Где бы инфраструктурные интерфейсы не были определены, в любом случае создается на них зависимость, пусть даже если она
не прямая, а проходит через дополнительный модуль.

Но возможен и третий вариант. Что если использовать для каждого инфраструктурного модуля те же подходы, что и для core
модуля, чтобы добиться его независимости от других инфраструктурных модулей? Что если использовать в нем для этих целей
шаблон наблюдатель, шаблон декоратор и контекст?

Обратимся к задаче с отправкой заказа. Для создания заказа в базе данных используется интерфейс `OrderRepository`.

```java
package aa0ndrey.dependency_inversion_guide.step_14.core.order;

public interface OrderRepository {
    void create(Order order);
}
```

Данный интерфейс имеет реализацию в postgres модуле.

Предположим, что потребовалось с помощью `TimeSpanManager` собирать информации о времени выполнения метода `create`.

Ниже представлен измененный интерфейс `TimeSpanManager`.

```java
package aa0ndrey.dependency_inversion_guide.step_14.application.time_span_manager;

public interface TimeSpanManager {
    void startTimeSpan(String name);

    boolean isActive();

    void addEvent(String name);

    void stopTimeSpan();
}
```

В интерфейс `TimeSpanManager` был добавлен метод
[addEvent](https://opentelemetry.io/docs/java/manual_instrumentation/#create-spans-with-events), который позволяет к
регистрируемому промежутку времени добавлять дополнительную информацию. Эта информация может носить вспомогательный
характер для анализа промежутков времени.

Пусть помимо регистрации времени выполнения метода `create` из класса `OrderRepositoryImpl`, посередине его выполнения
необходимо добавить информацию о собранном SQL-запросе, используя `addEvent` из интерфейса `TimeSpanManager`.

Скорее всего, читатель уже догадался, как для решения данной задачи будут совместно применены шаблон наблюдатель и
шаблон декоратор.

Рассмотрим для начала, как будет добавлена регистрация времени выполнения метода `create`. Для этого в application
модуль будет добавлен декоратор для `OrderRepository`.

```java
package aa0ndrey.dependency_inversion_guide.step_14.application.order;

public class OrderAppRepository implements OrderRepository { //(1)
    private final OrderRepository orderRepository; //(2)
    private final TimeSpanManager timeSpanManager;

    public void create(Order order) {
        try {
            timeSpanManager.startTimeSpan("Вставка заказа в таблицу"); //(3)
            orderRepository.create(order); //(4)
        } finally {
            if (timeSpanManager.isActive()) {
                timeSpanManager.stopTimeSpan(); //(5)
            }
        }
    }
}
```

В `(1)` класс `OrderAppRepository` также реализует интерфейс `OrderRepository`, так что он может быть использован в core
модуле, но не напрямую, а через интерфейс `OrderRepository`.

В `(2)` указан декорируемый объект с интерфейсом `OrderRepository`, который на самом деле является
классом `OrderRepositoryImpl` из postgres модуля.

В `(3)` и `(5)` вызываются методы для регистрации промежутка времени. А между ними в `(4)` вызывается метод `create`
декорируемого объекта.

Теперь рассмотрим, как будет добавлена информация о выполняемом SQL-запросе с помощью метода `addEvent`
из `TimeSpanManager`. Ниже представлен класс `OrderRepositoryImpl` из postgres модуля.

```java
package aa0ndrey.dependency_inversion_guide.step_14.postgres.order;

public class OrderRepositoryImpl implements OrderRepository {
    private final CreateOrderRepositoryObserverImpl observer; //(6)

    @Override
    public void create(Order order) {
        String sql = format( //(7)
                "insert into order (id, user_id, product_id) values (%s, %s, %s)",
                order.getId(),
                order.getUserId(),
                order.getProductId()
        );

        observer.afterSqlCreated(sql); //(8)

        executeSql(sql); //(9)
    }

    private void executeSql(String sql) {
        //отправка sql запроса
    }
}
```

В `(6)` находится поле наблюдателя, класс которого будет рассмотрен позже.

В `(7)` в упрощенном виде происходит создание SQL-запроса. В реальном проекте, по крайней мере на java, не стоит таким
образом собирать SQL-запрос, здесь лишь приведена упрощенная форма для компактности примера.

В `(8)` происходит отправка события с помощью наблюдателя, а в `(9)` отправляется SQL-запрос.

Ниже представлен класс наблюдателя для метода `create` из класса `OrderRepositoryImpl`

```java
package aa0ndrey.dependency_inversion_guide.step_14.application.order;

public class CreateOrderRepositoryObserverImpl {
    private final TimeSpanManager timeSpanManager;

    public void afterSqlCreated(String sql) {
        timeSpanManager.addEvent("Создан sql запрос для вставки заказа в таблицу: " + sql);
    }
}
```

Класс наблюдателя используется для того, чтобы добавить к регистрируемому промежутку времени информацию о выполняемом
SQL-запросе.

Отметим, что в данном примере в качестве параметра используется `String sql`, так как этого в данном случае достаточно.
Но при необходимости для метода `create` из `OrderRepository` можно было бы создать контекст, как это было сделано для
наблюдателей из `core` модуля в разделах 4, 5 и 6.

Данный пример был построен, исходя из предположения, что зависимость направлена от postgres модуля в сторону application
модуля.

В случае, если зависимость направлена наоборот, то тогда в postgres модуле необходимо добавить и использовать
интерфейс `CreateOrderRepositoryObserver`, реализация которого `CreateOrderRepositoryObserverImpl` все также будет
находиться в application модуле.

В результате получилось, что postgres модуль и open-telemetry модуль не имеют никаких зависимостей между собой. Все
взаимодействия между модулями были вынесены в application модуль. Так можно поступать для большинства инфраструктурных
модулей.

Отсюда следует, что в большинстве случаев можно выносить все взаимодействия между модулями в application модуль,
сохраняя высокую степень независимости каждого модуля от любых других инфраструктурных модулей. При этом связующая и
координирующая роль application модуля только возрастает.

Также необходимо отметить, что не всегда оправдано так изолировать каждый инфраструктурный модуль. Некоторые технологии
могут иметь удобные интеграции между собой, которые либо являются частью одной из технологий, либо подключаются в
качестве отдельных библиотек.

Если для использования готовой интеграции необходима явная зависимость между модулями, то в этом случае стоит связать
между собой эти модули явно.

Также стоит учитывать, что наличие готовой интеграции еще не означает, что ее невозможно провести через использование
шаблона наблюдатель и шаблона декоратор. Более того, в качестве механизма расширения некоторые технологии могут сами
предлагать использование этих шаблонов.

В качестве еще одного очевидного исключения можно также отметить использование логирования. Технически для каждого
вызова метода для записи логов, можно использовать шаблон наблюдатель с контекстом. Но это вряд ли будет оправдано, так
как слишком часто будет необходимо использовать наблюдателей. Более того, интерфейс для логирования достаточно прост и
устойчив к изменениям, что позволяет добавлять его в каждый инфраструктурный модуль.

Также очевидно, что не стоит под каждую библиотеку создавать отдельный независимый инфраструктурный модуль, если их
можно отнести к одному более общему инфраструктурному модулю. Например, в модуль postgres могут быть подключены
библиотеки относящиеся: к взаимодействию с СУБД postgres, к созданию SQL-запросов, к управлению пулом подключений, а
также остальные библиотеки, которые связаны с postgres или созданием SQL-запросов.

Настраивать взаимодействие между инфраструктурными модулями способом, описанным в данном разделе, следует тогда, когда
эти модули являются достаточно разными. В противном случае, возможно, их необходимо объединить в один модуль, либо
создать явную зависимость.

В итоге в рамках данного раздела было продемонстрировано то, что даже между инфраструктурными модулями возможно
организовывать взаимодействие через application модуль, применяя шаблон наблюдатель, шаблон декоратор и контекст. При
этом сохраняется независимость инфраструктурных модулей между собой, а координирующая и связующая роль application
модуля только возрастает.

### 15. Пример подключения библиотек и фреймворков к модулям

Предположим, что в разрабатываемом приложении есть 4 модуля:

- core
- application
- REST
- postgres

В рамках данного раздела рассмотрим стратегию по подключению библиотек и фреймворков к модулям. Начнем с core модуля.

Core модуль содержит основную логику, которая должна быть максимально изолирована от инфраструктурных особенностей и
используемых технологий. Лучше, чтобы в core модуле даже не были подключены какие-либо фреймворки общего назначения. В
противном случае при разработке основной логики нужно будет считаться с используемыми фреймворками.

Добавление фреймворка общего назначение может усложнить разработку тестов, ухудшить понимания происходящего в основной
логике и ухудшить возможности по смене версии фреймворка или по смене фреймворка целиком. Конечно, нельзя однозначно
утверждать, что всегда стоит избегать добавления фреймворков общего назначения в core модуль, но абсолютно точно стоит
учитывать те недостатки, к которым это приведет.

В частности, к [фреймворкам общего назначения](https://spring.io/) можно отнести такие фреймворки, которые помогают в
построении приложения, обеспечивая связи между различными его частями и навязывая определенный архитектурный стиль.

К фреймворкам общего назначения можно отнести большинство фреймворков, которые реализуют шаблон внедрение зависимостей (
dependency injection). Стоит отметить, что шаблон внедрения зависимостей помогает в реализации инверсии зависимостей, но
важно помнить, что это разные понятия.

Помимо фреймворков общего назначения, есть различные утилитные библиотеки, которые не навязывают архитектурный стиль,
которые могут быть использованы с любым фреймворком, и которые не связаны с какой-либо инфраструктурной технологией.

Например, [библиотеки](https://commons.apache.org/proper/commons-collections/) для работы с массивами
данных, [библиотеки](https://projectlombok.org/) упрощающие работу с данными, [библиотеки](https://mapstruct.org/)
упрощающие конвертацию (mapping) данных и другие. В некотором смысле эти библиотеки добавляют все те возможности,
которые могли бы быть в стандарте языка программирования. Использование утилитных библиотек допустимо в core модуле.

Перейдем к application модулю. Как раз в application модуле должен находится фреймворк общего назначения. Особенно если
это фреймворк по внедрению зависимостей, так как в этом смысле он отлично сочетается со связывающей и координирующей
ролью application модуля.

Отсюда также следует, что если в core модуле не был добавлен фреймворк общего назначения, то в application модуле должен
быть код, который интегрирует классы из core модуля с фреймворком общего назначения, в частности, добавит объекты
классов из core модуля в DI-контейнер.

И наконец перейдем к инфраструктурным модулям: REST и postgres. Здесь должны быть библиотеки, обеспечивающие работу
соответствующих инфраструктурных технологий. Для REST модуля это могут быть библиотеки, связанные с HTTP протоколом и с
работой с JSON форматом. Для postgres модуля это библиотеки, позволяющие взаимодействовать с СУБД postgres, позволяющие
создавать SQL-запросы и позволяющие извлекать результат выполнения SQL-запросов.

Также к инфраструктурным модулям может быть добавлен фреймворк общего назначения, особенно если во фреймворке общего
назначения есть удобные интеграции и реализации для различных технологий.

Данная стратегия по подключению библиотек и фреймворков к модулям позволяет не смешивать различные технологии и зоны
ответственности. Также в случае, если в core модуле не будет использовано никакого фреймворка, это позволит упростить
разработку и тестирование core модуля, а также позволит в будущем проще менять версии фреймворков или менять фреймворки
целиком.

### 16. Проведение аналогий

В данном разделе будут проведены аналогии на другие шаблоны проектирования и технологии преимущественно из
микросервисной архитектуры, с которыми, возможно, не каждый читатель знаком. К сожалению, в рамках данного раздела не
будет приведено детального описания для каждой технологии или шаблона, а лишь будут указаны ссылки, где можно получить
подробную информацию.

Если что-то останется непонятным, то это никак не отразиться на общем понимании статьи, так как данный раздел носит
вспомогательный характер.

Представим, что мы перейдем от рассмотрения архитектуры одного приложения к микросервисной архитектуре. Какие можно
заметить аналогии с тем, что было использовано ранее в архитектуре одного приложения?

Можно начать, возможно, с самого очевидного:
с [брокеров сообщений](https://microservices.io/patterns/communication-style/messaging.html). Для того чтобы обеспечить
низкую связность между различными микросервисами, могут быть использованы брокеры сообщений с
применением [событийно-ориентированной архитектуры](https://microservices.io/patterns/data/event-driven-architecture.html)
для микросервисов, что аналогично использованию шаблона наблюдатель.

Более того, в событийно-ориентированной архитектуре можно выделить два
подхода: [хореография и оркестрация](https://codeopinion.com/event-choreography-orchestration-sagas/). Использование
хореографии аналогично тому, как ранее в первой части статьи в примерах приложения каждый инфраструктурный модуль
реализовывал внутри себя наблюдателей. В свою очередь, использование оркестрации аналогично использованию модуля
application для централизации логики.

Примечательно то, что те недостатки, которые выделяются в хореографии, связанные с неявным управлением, решаются с
помощью использования оркестрации, что аналогично тому, как недостатки с помощью нескольких наблюдателей в
инфраструктурных модулях решались размещением единственного наблюдателя в application модуле.

Можно заметить, что application модуль хорошо сопоставляется с теми или иными решениями из микросервисной архитектуры,
которые предполагают координацию нескольких микросервисов. Сопоставить application модулю также можно и шаблон
[process manager](https://www.enterpriseintegrationpatterns.com/patterns/messaging/ProcessManager.html)
из [шаблонов корпоративных интеграций](https://www.enterpriseintegrationpatterns.com/).

А есть ли среди подходов и шаблонов для микросервисов что-то похожее на шаблон декоратор? Да, есть, например,
прокси-сервера, которые могут расширять функции основного сервиса, за счет того, что сетевой трафик проходит целиком
через них. Прокси-сервера могут взять на себя часть обязанностей по кэшированию, логированию и безопасности, расширяя
тем самым возможности основного сервиса, подобно использованию шаблона декоратор.

В рамках данной статьи было уделено мало внимания использованию данных. Тем не менее для модуля core, для application
модуля, если не используется infrastructure модуль, и для infrastructure модуля, если он используется, можно отметить,
что они берут на себя ответственность по хранению и стандартизации интерфейсов и классов-данных, используемых между
модулями. Это в свою очередь аналогично использованию
шаблона [каноничных моделей данных](https://www.enterpriseintegrationpatterns.com/patterns/messaging/CanonicalDataModel.html)
из шаблонов корпоративных интеграций.

Построение подобных аналогий полезно для переиспользования опыта, подходов и шаблонов из разных областей знаний, а также
это позволяет лучше раскрыть представленный материал и идеи.

### 17. Подведение итогов

Подведем итог и рассмотрим, что было представлено во второй части.

1. Было продемонстрировано использование шаблона наблюдатель и шаблона декоратор с помощью application модуля.
2. Была определена основная ответственность application модуля, заключающаяся в координации взаимодействий всех
   остальных модулей.
3. Были рассмотрены различные варианты направления зависимостей и то, с какими проблемами возможно придется столкнуться
   при выборе того или иного варианта направления зависимостей.
4. Была предложена стратегия для направления зависимостей, подразумевающая, что все зависимости изначально стоит
   направлять в сторону application модуля, так как изменить направление в этом случае проще, чем обратно.
5. Было продемонстрировано использование шаблона наблюдатель и шаблона декоратор для изолирования инфраструктурных
   модулей друг от друга, что должно положительно сказывается на соблюдении зон ответственности каждого модуля.
6. Была представлена стратегия размещения библиотек и фреймворков между модулями.
7. Были проведены аналогии на подходы и шаблоны из микросервисной архитектуры.

Данный раздел является последним в рамках данного руководства, а нераскрытая тема использования чистых функций для core
модуля будет представлена в качестве отдельной статьи. Я, aa0ndrey, надеюсь, что кому-то мой опыт и раскрытие
данной темы оказались полезными.

[Ссылка](https://github.com/aa0ndrey/dependency-inversion-guide/tree/habr) на github репозиторий.