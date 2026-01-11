## Section 1: Azure Fundamentals

### Task 1: Draw one end-to-end architecture planning

users

compute

database

networking
Monitoring & Security
CI/CD

Order of the plan for diagrams
Users
↓
Azure Front Door / Load Balancer
↓
App Service / Containers
↓
Database
↓
Monitoring & Logs

### Explanations of choices made above

#### Azure Fundamentals – Service Selection

##### Users

The system is accessed by traders and internal users through standard web browsers. Users are shown outside the Azure boundary to clearly indicate the trust and responsibility separation between clients and the cloud environment.

##### Edge / Networking – Azure Front Door

###### Why I have chosen Azure Front Door instead of Load Balancer

Azure Front Door is used as the global entry point for the application. It provides:

- Secure internet-facing access

- Traffic distribution

- High availability for users across multiple geographic regions

- This aligns with the requirement for 24/7 global trading operations.

###### Alternative considered

Azure Load Balancer was considered as a simpler, regional alternative. However, it does not provide global routing or application-layer capabilities, making it less suitable for a global enterprise-facing web application.

##### Application Layer – Azure App Service (for Containers)

Azure App Service (for Containers) is used to host the containerised Spring Boot application. This provides:

- A managed PaaS environment
- Automatic scaling
- Reduced operational overhead
- Seamless integration with Azure monitoring and security services

Docker is used as the application runtime, ensuring consistency between development and cloud deployment.

###### Alternative considered

Virtual Machines were considered but rejected due to:

- Higher operational responsibility
- Manual patching and scaling
- Increased infrastructure management effort

##### Data Layer – Azure SQL Database

###### Why chosen SQL

Azure SQL Database is used as the primary data store for trade data. It provides:

- A fully managed relational database service
- Strong consistency guarantees
- Enterprise-grade security and compliance features
- Deep integration with the Azure ecosystem
- This makes it well-suited for financial services workloads.

###### Alternatives Databses considered

- Azure Database for PostgreSQL (open-source compatibility)
- Azure Cosmos DB (global scale, low latency)

These alternatives were not selected as the system primarily requires structured relational data with strong consistency rather than global multi-model distribution.

#### Operations, Monitoring, and Security

##### Operations & Monitoring

The solution uses:

- Azure Monitor
- Application Insights
- Microsoft Defender for Cloud

Together, these services provide:

- Application and infrastructure observability
- Security posture management
- Incident detection and alerting

This supports operational reliability and regulatory expectations in financial environments.

##### CI/CD & DevOps

CI/CD Strategy – Azure DevOps & Azure Container Registry
Azure DevOps is used to manage CI/CD pipelines, with Azure Container Registry storing container images.

This enables:

- Automated build and deployment
- Consistent release processes
- Integration with Git-based source control

###### Alternative considered

GitHub Actions is a viable alternative for organisations standardised on GitHub; however, Azure DevOps is commonly preferred in enterprise environments.

### Virtual Machines

Virtual Machines were intentionally not included in the initial architecture, as the application is designed using a PaaS model with Azure App Service for Containers. This reduces operational overhead while still allowing scalability and enterprise-grade reliability. Virtual Machines could be introduced later if specific legacy or specialist workloads require them.

# Incomplete for following sections plan only

In a theoretical enterprise design…
Exact sizing would be determined during implementation…
This design prioritises scalability over fixed capacity…
Assumptions & Constraints @ UBS Trading Cloud
Global trading platform with millions of daily transactions
Peak trading during market open/close windows
Data retention driven by financial compliance
Zero-downtime requirement during business hours
Designed for horizontal scalability rather than fixed sizing

## Links used to learn more about how to design the cloud architecture

https://learn.microsoft.com/en-us/azure/well-architected/architect-role/architecture-design-specification

https://learn.microsoft.com/en-us/azure/well-architected/architect-role/design-diagrams

https://learn.microsoft.com/en-us/azure/architecture/icons/
