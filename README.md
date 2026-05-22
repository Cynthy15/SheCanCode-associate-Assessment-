# Idempotency Gateway - IgirePay Technologies

A robust payment processing system with idempotency guarantees to prevent double-charging.

## Problem Statement

Payment processors face a critical challenge: network timeouts cause clients to retry requests, leading to double-charging. This system ensures that duplicate requests are safely handled, processing payments exactly once.

## Architecture

### System Flow

sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant Database

    Client->>Controller: POST /process-payment<br/>Header: Idempotency-Key<br/>Body: {amount, currency}
    
    Controller->>Controller: Validate Headers & Body
    Controller->>Service: processPayment(key, request)
    
    Service->>Service: Convert request to JSON<br/>Generate SHA-256 hash
    
    Service->>Repository: findByIdempotencyKeyWithLock(key)
    Repository->>Database: SELECT with PESSIMISTIC_WRITE lock
    
    alt Record Exists
        Database-->>Repository: Return existing record
        Repository-->>Service: IdempotencyRecord found
        
        alt Request Hash Matches
            alt Status = COMPLETED
                Service->>Service: Deserialize cached response
                Service-->>Controller: Return cached response<br/>X-Cache-Hit: true
            else Status = PROCESSING
                Service->>Service: Wait for lock release<br/>(In-flight handling)
                Service->>Repository: Re-fetch record
                Service-->>Controller: Return result
            end
        else Request Hash Different
            Service-->>Controller: Throw IdempotencyKeyMismatchException
            Controller-->>Client: 422 Unprocessable Entity
        end
        
    else Record Does Not Exist
        Service->>Repository: save(new record with PROCESSING status)
        Repository->>Database: INSERT record
        
        Service->>Service: Execute payment processing<br/>(2 second delay)
        
        Service->>Service: Generate PaymentResponse<br/>with transaction ID
        
        Service->>Repository: Update record<br/>(save response, set COMPLETED)
        Repository->>Database: UPDATE record
        
        Service-->>Controller: Return response<br/>X-Cache-Hit: false<br/>Status: 201 Created
    end
    
    Controller-->>Client: HTTP Response with headers

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
