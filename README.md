# Idempotency Gateway - IgirePay Technologies

A robust payment processing system with idempotency guarantees to prevent double-charging.

## Problem Statement

Payment processors face a critical challenge: network timeouts cause clients to retry requests, leading to double-charging. This system ensures that duplicate requests are safely handled, processing payments exactly once.

## Architecture

### System Flow


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
