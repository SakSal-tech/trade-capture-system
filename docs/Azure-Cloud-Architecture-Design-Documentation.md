# Azure Cloud Architecture Design

This document reflects architectural reasoning and learning rather than an internal UBS production design.
This document describes the Azure cloud architecture design for an enterprise-style Trade Capture System developed as part of a banking software engineering programme in collaboration with UBS.

The purpose of this document is to demonstrate understanding of Azure fundamentals and how they can be applied to a real trading system through architectural reasoning, service selection, and documented trade-offs. The design is theoretical and focuses on architecture rather than live deployment or exact sizing.

# Initial Architecture Plan and Design Outline

The following section represents the initial architectural planning stage. It was used to reason about system structure, responsibilities, and service boundaries before converging on the final architecture.

## The draft focused on identifying:

- Core application layers
- Data persistence requirements
- Compute options
- Deployment and operational considerations

Arrows and high-level components were intentionally used at this stage to visualise flow and dependencies before refining the architecture into layered Azure services.
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

This initial plan informed the final design decisions documented in the sections below and is retained to demonstrate architectural progression.

# Section 1: Azure Fundamentals

## 1.1 Core Azure Services for the Trade Capture System

The architecture uses the following Azure services to support an enterprise trading application:

- Azure Front Door as the global entry point for user traffic
- Azure App Service (for Containers) to host the backend API and application runtime
- Azure SQL Database as the primary relational data store
- Azure Monitor and Application Insights for monitoring, logging, and diagnostics
- Microsoft Defender for Cloud for security posture management
- Azure DevOps for source control integration and CI/CD pipelines
- Azure Container Registry for storing and managing container images

These services align with Azure Fundamentals concepts and support scalability, availability, security, and operational efficiency in a regulated financial environment such as UBS.

## 1.2 Compute Options Comparison

Azure offers multiple compute models, each with different trade-offs.

Virtual Machines provide full control over the operating system and runtime. They are commonly used in enterprise environments for legacy systems or specialised workloads. However, they require manual management of patching, scaling, availability, and security hardening, increasing operational complexity.

Azure App Service provides a Platform as a Service model that abstracts infrastructure management. It is well suited for web applications and APIs, offering built-in scaling, availability, and integration with Azure monitoring and security services.

Containers package applications and their dependencies together, ensuring consistency across environments and reducing reliance on specific operating systems or hardware.

Azure Kubernetes Service provides advanced container orchestration capabilities. It is suitable for large microservice platforms requiring complex deployment strategies, service discovery, and orchestration at scale.

Azure Functions support event-driven serverless workloads and are well suited for background processing and integration tasks, but not as the primary runtime for a core trading API.

## 1.3 Why Azure App Service for Containers Was Selected

In a theoretical enterprise design, large organisations may operate Kubernetes platforms for complex microservice workloads. This architecture deliberately uses Azure App Service for Containers to reduce operational complexity while still supporting scalability and enterprise requirements.

The Trade Capture System exposes a Spring Boot REST API with well-defined endpoints for trade search, validation, dashboards, and settlement workflows. This workload maps cleanly to a managed PaaS web application model.

Kubernetes-based platforms could be introduced if the application evolved to require advanced orchestration across many independently deployed services. At the current stage, App Service provides the required scalability and reliability without introducing unnecessary platform complexity.

## 1.4 Resource Responsibility Model

The design follows a PaaS-first approach aligned with Azure Fundamentals. Azure manages infrastructure concerns such as operating system patching, platform availability, and baseline security, while the application team focuses on business logic, financial validation rules, and API behaviour.

This aligns with the shared responsibility model and reduces operational risk in a financial services environment.

# Section 2: Architecture Design

## 2.1 End-to-End Azure Architecture

The following diagram shows the high-level Azure cloud architecture for the Trade Capture System. It illustrates how users access the platform, how the application is hosted and scaled, and how data, monitoring, and DevOps tooling integrate within Azure.

The system is accessed by UBS traders and internal users through standard web browsers.  
Users are shown outside the Azure boundary to clearly indicate the trust and responsibility separation between client environments and the Azure cloud platform.

### Architecture Diagrams

- End-to-end Azure architecture

  - [`docs/diagrams/Azure-Architecture.drawio.png`](docs/diagrams/Azure-Architecture.drawio.png)

- Editable source
  - [`docs/diagrams/Azure-end-to-end-architecture.drawio`](docs/diagrams/Azure-end-to-end-architecture.drawio)

## 2.2 Application Layer Design

User traffic enters the system through Azure Front Door, which acts as the secure global entry point. This supports global access patterns and improves availability for users across regions.

The backend Spring Boot API is deployed using Azure App Service for Containers. The application is packaged as a Docker container and runs on a managed platform that supports horizontal scaling and high availability.

Docker is used as the runtime mechanism to ensure consistency between development and cloud environments. It is not treated as middleware but as a packaging and runtime technology.

### Azure App Service (for Containers)

Azure App Service (for Containers) is used to host the containerised Spring Boot application. This provides:

- A managed PaaS environment
- Automatic scaling
- Reduced operational overhead
- Seamless integration with Azure monitoring and security services

Docker is used as the application runtime, ensuring consistency between development and cloud deployment.

### Alternative Considered

Virtual Machines were considered but rejected due to:

- Higher operational responsibility
- Manual patching and scaling
- Increased infrastructure management effort

## 2.3 Data Layer Design and Database Comparison

### Why SQL Database

Azure SQL Database is used as the primary data store. It provides a fully managed relational database service with strong transactional consistency, high availability, and enterprise-grade security features suitable for financial data.

### ### Alternative Considered

- Azure Database for PostgreSQL offers open-source compatibility and flexibility.
- Azure Cosmos DB provides global distribution and low-latency access for multi-model data.

These alternatives were not selected because the Trade Capture System primarily requires structured relational data, strong consistency, and compliance-aligned data management rather than global multi-model distribution.

## 2.4 Networking and Security Design

Azure Front Door provides a secure public entry point and helps reduce direct exposure of backend services. Microsoft Defender for Cloud provides visibility into security posture and configuration risks.

Access to Azure resources is conceptually controlled through role-based access control. Network design is intentionally high level, as detailed subnetting and firewall configuration would be determined during implementation.

### Why I have chosen Azure Front Door instead of Load Balancer

Azure Front Door is used as the global entry point for the application. It provides:

- Secure internet-facing access
- Traffic distribution
- High availability for users across multiple geographic regions
- This aligns with the requirement for 24/7 global trading operations.

### Alternative Considered

Azure Load Balancer was considered as a simpler, regional alternative. However, it does not provide global routing or application-layer capabilities, making it less suitable for a global enterprise-facing web application.

## 2.5 Assumptions and Constraints for UBS Trading Cloud

This architecture is based on the following assumptions and constraints:

- Global trading platform with millions of daily transactions
- Peak trading volumes during market open and close windows
- Data retention driven by financial compliance requirements
- Zero-downtime requirement during business hours
- Designed for horizontal scalability rather than fixed sizing

Exact sizing would be determined during implementation based on observed workloads, performance testing, and compliance requirements. This design prioritises scalability over fixed capacity to support variable trading demand.

# Section 3: Deployment Strategy

## 3.1 Container Strategy

The Spring Boot API is containerised using Docker to ensure consistent runtime behaviour across environments. Container images are built through the CI/CD pipeline and stored in Azure Container Registry.

This approach supports repeatable deployments and clear separation between application code and runtime configuration.

## 3.2 CI/CD and DevOps Strategy

The CI/CD strategy for the Trade Capture System is based on a Git-driven automation pipeline that builds, tests, and deploys containerised applications in a repeatable and controlled manner.

Source code changes trigger automated workflows that execute unit and integration tests, build Docker images, and publish versioned images to a container registry. Approved images are then deployed to the target runtime environment.

This architecture is intentionally tool-agnostic at the design level. In practice, the CI/CD workflow can be implemented using platforms such as Azure DevOps or GitHub Actions, depending on repository hosting and organisational standards.

As part of ongoing hands-on practice, GitHub Actions is planned for implementing the full CI/CD pipeline, including automated testing and Docker-based deployment, building on the existing containerisation work.

## 3.3 Scaling and Performance Strategy

The application is designed for horizontal scalability using Azure App Service scaling capabilities. Capacity is adjusted dynamically in response to demand rather than through fixed sizing.

This design prioritises scalability over fixed capacity and supports variable trading volumes without manual intervention.

## 3.4 CI/CD and DevOps Tooling

Azure DevOps is used to manage continuous integration and deployment pipelines, with Azure Container Registry storing versioned container images for the application.

This approach enables automated build and deployment, consistent release processes, and integration with Git-based source control. It supports controlled releases, auditability, and repeatable deployments suitable for an enterprise trading system.

### Alternative Considered

GitHub Actions is a viable alternative for organisations standardised on GitHub. However, Azure DevOps is commonly preferred in enterprise environments due to its integrated tooling, governance features, and alignment with Azure-based delivery platforms.

# Section 4: Operations and Monitoring

## Operations, Monitoring, and Security

The Trade Capture System is monitored using Azure Monitor, Application Insights, and Microsoft Defender for Cloud to ensure reliability, security, and operational visibility appropriate for a trading environment.

These services provide application and infrastructure observability, security posture management, and proactive incident detection. In the context of a UBS trading system, this enables rapid identification of issues that could impact traders during active trading windows, such as degraded API performance or failed trade submissions.

Operational alerts support timely investigation and remediation by engineering and operations teams, reducing operational risk and potential business disruption. Deployment pipelines provide a controlled rollback mechanism if a release introduces issues during trading hours.

The use of managed platform services and database backup capabilities supports business continuity and recovery objectives, helping ensure system availability and data protection in line with financial services operational and regulatory expectations.
