# E-Commerce Microservices - Complete API Testing & Execution Guide

Now that we have built an industry-standard e-commerce backend with specialized functionality, it's time to run it and test how these services talk to each other!

---

## 🏗️ PART 1: The Startup Order

Because microservices rely on each other, you **must** start them in the correct order. Open your IDE (IntelliJ/Eclipse) or Terminal and start the Spring Boot applications as follows:

1. **Config Server (`config-server`)**: Wait for it to show _Started ConfigServerApplication_.
2. **Eureka Server (`eureka-server`)**: Wait for it to spin up. (You can visit `http://localhost:8761` in your browser to see the Eureka Dashboard).
3. **User Service (`user-service`) & Product Service (`product-service`)**: Start these simultaneously. They don't depend on each other.
4. **Order Service (`order-service`)**: Start this _after_ User and Product services are running, because it needs to find them in Eureka for Feign Clients.
5. **API Gateway (`api-gateway`)**: Start this last.

_Wait about 30 seconds after starting everything. If you refresh the Eureka Dashboard (`http://localhost:8761`), you should see all 4 services registered (GATEWAY, USER-SERVICE, PRODUCT-SERVICE, ORDER-SERVICE)._

---

## 🚀 PART 2: Exploring the APIs (The E-Commerce Flow)

We will make all our API calls through the **API Gateway (Port 8080)**. The Gateway will automatically route our requests to the correct underlying service using Eureka.

You can use **Postman**, **cURL**, or **Thunder Client** inside VSCode to run these tests.

### Step 1: Create a Customer (Talks to User Service)

Let's register a new user in the system.

- **Method:** `POST`
- **URL:** `http://localhost:8080/users/register`
- **Body (JSON):**
  ```json
  {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "address": "123 Main St, New York"
  }
  ```
- **Behind the scenes:** The Gateway sees `/users/` and forwards this to the `user-service` (Port 8081).
- **Note the returned `id` (likely `1`).**

### Step 2: Add Inventory (Talks to Product Service)

Now, let's stock up our warehouse with an expensive laptop.

- **Method:** `POST`
- **URL:** `http://localhost:8080/products`
- **Body (JSON):**
  ```json
  {
    "name": "MacBook Pro M3",
    "price": 1999.99,
    "quantity": 5
  }
  ```
- **Behind the scenes:** Gateway forwards to `product-service` (Port 8082).
- **Note the returned `id` (likely `1`).**

### Step 3: Check Available Store Products

A customer wants to see what's in stock.

- **Method:** `GET`
- **URL:** `http://localhost:8080/products/available`
- **Expected Result:** You will see the MacBook Pro with quantity `5`.

### Step 4: THE MAGIC - The Checkout Process (Inter-Service Communication)

John Doe wants to buy 2 MacBooks. Let's trigger the checkout.

- **Method:** `POST`
- **URL:** `http://localhost:8080/orders/checkout`
- **Body (JSON):**
  ```json
  {
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }
  ```

**🚨 WHAT JUST HAPPENED BEHIND THE SCENES here?**
This is the core of microservices architecture:

1. The **Gateway** routes the request to the `order-service` (Port 8083).
2. The `order-service` uses OpenFeign to make an internal HTTP call to `user-service` to verify User #1 exists and grab their address.
3. The `order-service` uses OpenFeign to call `product-service` to verify Product #1 has at least 2 items in stock.
4. The `order-service` calculates the total price: 2 \* $1999.99 = $3999.98.
5. **Inventory Reduction:** The `order-service` makes a `PUT` request via Feign to `product-service` to subtract 2 from the MacBook inventory.
6. The `order-service` saves the completed order and returns your receipt.

### Step 5: Verify Inventory Reduction

Let's make sure the inventory actually went down!

- **Method:** `GET`
- **URL:** `http://localhost:8080/products/1`
- **Expected Result:** The `quantity` for the MacBook Pro should now be exactly `3` instead of `5`. The Order Service successfully updated a table in the Product Service's database!

---

## 🛠️ PART 3: Testing the Circuit Breaker (Resilience4j)

Microservices are meant to be fault-tolerant. What happens if the `product-service` crashes while a user is trying to check out? Let's test it.

1. **Simulate a Crash:** Go to your IDE/terminal and completely **STOP** the `product-service` application.
2. Wait about 30 seconds for the Gateway and Eureka to realize it's down.
3. **Attempt Checkout Again:** Run the exact same order POST request from Step 4.
   ```json
   {
     "userId": 1,
     "productId": 1,
     "quantity": 1
   }
   ```

**The Result:**
Instead of getting an ugly `500 Internal Server Error` and a massive Java stack trace, Resilience4j intercepts the failed connection and executes the `productFallback` method in `OrderService.java`.
You will receive a graceful, user-friendly JSON response:

```json
{
  "status": "FAILED",
  "message": "Sorry! Checkout is temporarily unavailable as the Inventory system is down. Please try again later. Error: Connection refused..."
}
```

This prevents cascading failures where one dead service brings down the entire E-Commerce platform!
