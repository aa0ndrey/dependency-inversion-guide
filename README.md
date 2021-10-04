Инверсия зависимостей - один из принципов SOLID, который лежит в основе построения гексагональной архитектуры
приложения. Существует множество статей, которые раскрывают суть принципа и объясняют как его применять. И, возможно,
читатель уже знаком с ними. Но в рамках данной статьи будет продемонстрирован подробный разбор "тактических" приемов
для успешного использования инверсии зависимостей и, возможно, в этом смысле даже искушенный читатель сможет найти для
себя что-то новое. Примеры представлены на языке программирования Java с соответствующим окружением, но при этом для
чтения достаточно понимания похожих языков программирования.

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

Чтобы решить проблему, обозначенную в предыдущем разделе, достаточно в core модуле создать интерфейсы для репозиториев, а
сами репозитории из postgres модуля заставить реализовывать указанные интерфейсы.  
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

Чего в итоге удалось добиться? За счет использования контекста между двумя изолированными методами для обработки
событий внутри инфраструктурного модуля удалось передать данные. При этом за счёт обобщенного поля `data` внутри
контекста детали реализации инфраструктурного модуля не проникли в изолируемый модуль (core). Тут является очень важным,
что в контексте не появилось конкретного поля, такого как `long transactionId`, которое раскрывало бы детали
реализации.

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
не стоит также забывать о том, что точек для отправки событий с использованием контекста можно добавить сколько угодно
в метод с основной логикой, даже посередине, а не только в начале и конце. При этом не придется даже создавать отдельные
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
в каком направлении необходимо организовать передачу данных, можно использовать то или иное решение. Универсальным, но не
всегда оптимальным, механизмом для передачи данных является использование контекста.

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