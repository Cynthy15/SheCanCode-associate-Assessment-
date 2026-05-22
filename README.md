# Idempotency Gateway - IgirePay Technologies

A robust payment processing system with idempotency guarantees to prevent double-charging.

## Problem Statement

Payment processors face a critical challenge: network timeouts cause clients to retry requests, leading to double-charging. This system ensures that duplicate requests are safely handled, processing payments exactly once.

## Architecture Flowchart

```mermaid
flowchart TD

A[Client Sends Payment Request] --> B[API Gateway / Spring Boot Controller]

B --> C{Idempotency-Key exists?}

C -- No --> D[Validate Request Body]
D --> E[Start Payment Processing]
E --> F[Simulate Processing Delay 2s]
F --> G[Charge Customer]
G --> H[Save Response + Idempotency Key]
H --> I[Return 201 Created + Response]

C -- Yes --> J{Same Request Body?}

J -- Yes --> K[Return Cached Response]
K --> L[Add Header X-Cache-Hit: true]

J -- No --> M[Return 409 / 422 Error]
M --> N["Idempotency key reused with different payload"]

```

---

## Sequence Diagram 

```mermaid
sequenceDiagram

participant Client
participant API
participant Database
participant Lock
participant PaymentService

Client->>API: POST /process-payment (Idempotency-Key)

API->>Database: Check if key exists

alt First Request
Database-->>API: Key not found

API->>Database: Save request as PROCESSING
API->>Lock: Acquire lock for key
API->>PaymentService: Process payment (2s delay)
PaymentService-->>API: Payment success
API->>Database: Save response + mark COMPLETED
API->>Lock: Release lock
API-->>Client: 201 Created (Charged 100 RWF)

else Duplicate Request (Same Payload)
Database-->>API: Key exists
API->>Database: Fetch stored response
API-->>Client: Return cached response
Note over API: X-Cache-Hit: true

else Same Key Different Payload
Database-->>API: Key exists
API-->>Client: 409 Conflict
end

```
### Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Key Libraries**: Spring Data JPA, Lombok, Jackson

## Setup Instructions

### Prerequisites

- Java 17 or higher
- PostgreSQL 12+
- Maven 3.6+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/idempotency-gateway.git
   cd idempotency-gateway
