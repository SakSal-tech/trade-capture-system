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

Azure SQL Database is selected as the primary data store for the Trade Capture System. Core trading data such as trades, trade legs, counterparties, books, and settlement instructions require strong transactional consistency and well-defined relationships. These characteristics align well with a relational database model.
It works well with other Azure services used for reporting and data analysis services such as PowerBi and Analysis Services which are needed for this Trade system.

### Hybrid Data Considerations (NoSQL)

A hybrid data approach is realistic for an enterprise trading system. While Azure SQL is used as the system of record for core trading data, NoSQL databases such as Azure Cosmos DB could be used for supporting data that is semi-structured or high-volume.

Examples include trade event history documents, operational notifications, and workflow state snapshots. These data types do not require foreign key relationships and benefit from flexible schemas and low-latency access. Cosmos DB was not selected as the primary database, as core trade data requires strict relational consistency.

### Alternative Considered

Azure Database for PostgreSQL was considered as an alternative. While PostgreSQL is a capable relational database, Azure SQL is commonly used in enterprise financial environments and integrates closely with Microsoft-based platforms. For this system, Azure SQL provides a familiar and robust foundation for structured trading data.

Azure Cosmos DB provides global distribution and low-latency access for multi-model data.

These alternatives were not selected because the Trade Capture System primarily requires structured relational data, strong consistency, and compliance-aligned data management rather than global multi-model distribution.

## 2.4 Networking and Security Design

Azure Front Door provides a secure public entry point and helps reduce direct exposure of backend services. Microsoft Defender for Cloud provides visibility into security posture and configuration risks.

Access to Azure resources is conceptually controlled through role-based access control. Network design is intentionally high level, as detailed subnetting and firewall configuration would be determined during implementation.

### Why I Chose Azure Front Door Instead of Azure Load Balancer

I use Azure Front Door as the global entry point for the Trade Capture System. This decision is driven by the need to support a secure, highly available, internet-facing trading application used by traders and internal users across multiple geographic regions.

Azure Front Door operates at the application layer (Layer 7) and handles HTTP and HTTPS traffic. It provides:

- Secure internet-facing access over HTTPS
- Global traffic routing at the Microsoft edge
- Application-layer request handling and routing
- High availability and resilience across regions
- A single public entry point for all users

For a trading system, this ensures that traders can reliably access the platform during critical market windows while backend services remain protected behind managed entry points. This aligns with the requirement for 24/7 global trading operations and predictable access patterns.

### Alternative Considered: Azure Load Balancer

I considered Azure Load Balancer as a simpler alternative. Azure Load Balancer operates at the transport layer (Layer 4) and distributes TCP and UDP traffic across backend instances.

While this approach works well for VM-based workloads and internal service-to-service traffic, it does not provide application-layer capabilities such as HTTP routing, request inspection, or global traffic distribution. Azure Load Balancer is also regional, which makes it less suitable as the primary entry point for a globally accessed trading application.

For these reasons, Azure Load Balancer was not selected as the main entry service for the Trade Capture API.

### Related Entry Options in a Large-Scale Banking Environment (Application Gateway)

In large banking environments such as UBS, multiple traffic management components are often combined depending on system scale and security requirements.

Azure Application Gateway operates at the application layer (Layer 7) within a Virtual Network and supports HTTP and HTTPS traffic with advanced routing and Web Application Firewall capabilities. It is commonly used when backend services must remain on private IP addresses and traffic inspection is required before requests reach internal applications.

In a VM-based or highly segmented network design, Azure Load Balancer is commonly used for internal traffic distribution between private backend services. In this model, Azure Load Balancer operates on private IP addresses inside a Virtual Network and is not exposed to the internet. Public access is instead handled by application-layer services such as Azure Front Door or Azure Application Gateway. For this project, I did not select Azure Load Balancer because the Trade Capture System is hosted on managed PaaS services and does not require internal VM-to-VM traffic distribution.

For this project, I chose Azure Front Door as the primary public entry point to keep the architecture simple while still aligning with enterprise patterns. If the Trade Capture System evolved to require stricter network isolation or deeper inspection within a Virtual Network, Application Gateway could be introduced behind Front Door to protect internal application endpoints.

### Public and Internal IP Considerations

In this architecture, Azure Front Door exposes a single public endpoint to traders and internal users. Backend services, including the Trade Capture API and databases, are not directly exposed to the internet.

In larger enterprise deployments, backend services typically use internal IP addresses and are accessed through private endpoints or internal load balancers. This separation ensures that sensitive trade and settlement services remain isolated while still being reachable through approved, controlled entry points.

### Virtual Machines as an Enterprise Alternative

I selected Azure App Service as the primary compute platform for this Trade Capture System, but I also considered Virtual Machines because they are commonly used in large trading environments such as UBS when additional control or isolation is required.

For a trade capture and settlement platform, I would choose Virtual Machines if backend components required operating system-level access. For example, proprietary risk engines, trade surveillance tools, or compliance and audit agents often need to be installed directly on the host operating system. These types of components are frequently used to monitor trading activity, enforce regulatory controls, or integrate with legacy reporting systems, and they cannot run within managed PaaS environments.

If I hosted the Trade Capture API on Virtual Machines, I would design the network to enforce strict separation between system layers. I would place any internet-facing entry components in a DMZ subnet, deploy the trade processing API into a private application subnet behind an internal load balancer, and restrict database access to a dedicated data subnet. I would apply Network Security Groups to each subnet to ensure that only approved traffic paths can access sensitive trade, settlement, and reference data.

I would also use Azure Bastion to manage administrative access to the Virtual Machines. This would allow engineers or operations teams to access trade system hosts securely without exposing public IP addresses, which is important in environments handling sensitive financial data.

For this project, I did not choose Virtual Machines because the Trade Capture API is a stateless, containerised Spring Boot service that does not require operating system customisation or proprietary host-level software. Using Azure App Service allows me to reduce operational overhead while still meeting the security, scalability, and availability requirements of a trading application.

### DDoS Protection, Firewall, and Network Isolation

Because this Trade Capture System exposes an internet-facing API used by traders and internal users, I considered how to protect it against external attacks while limiting access to sensitive backend resources.

For denial-of-service protection, I rely on Azure’s built-in DDoS protection for public-facing endpoints. This protects the Trade Capture API from large-scale traffic floods that could prevent traders from submitting or amending trades during active trading windows. Using a managed service avoids the need to implement custom DDoS mitigation logic at the application level.

I also considered the use of Azure Firewall in scenarios where stricter network control is required. For example, if the trading system needed to tightly restrict outbound traffic or inspect traffic between application components, Azure Firewall would allow centralized control, logging, and enforcement of security policies. In this model, backend services such as the Trade Capture API and settlement-related components would remain on private IP addresses and be accessible only through approved entry points.

For this project, I did not make Azure Firewall a mandatory component because the primary API is hosted on managed PaaS services that already reduce the attack surface. Instead, I focused on keeping backend services private and exposing only a controlled application entry point for trader access. This layered approach balances security with operational simplicity while still supporting the access control and isolation expected in a trading environment.

### Content Delivery Network (CDN) Considerations

At the current stage of this project, the Trade Capture System focuses primarily on the backend API and its integration with cloud services. The API serves dynamic trade, settlement, and validation data, which is not suitable for CDN caching. For this reason, a CDN is not required for the backend API itself.

I considered CDN usage primarily in the context of the frontend application. If the React-based trading UI is deployed as a cloud-hosted web application, a CDN becomes beneficial for serving static assets such as HTML, JavaScript, CSS, and images. These assets change infrequently and are accessed by all users, making them ideal candidates for edge caching.

In a trading environment where users may access the system from different geographic regions, a CDN can reduce latency and improve initial page load times for the frontend. The CDN would cache static frontend assets at edge locations, while all dynamic trade operations would continue to be handled directly by the Trade Capture API.

For this project, I have not made CDN a mandatory component because the primary focus is backend architecture and API design. However, if the frontend is deployed as part of the system, I would integrate a CDN in front of the static web assets while keeping the API endpoints uncached to ensure data accuracy and consistency.

## 2.5 Assumptions and Constraints for UBS Trading Cloud

This architecture is based on the following assumptions and constraints:

- Global trading platform with millions of daily transactions
- Peak trading volumes during market open and close windows
- Data retention driven by financial compliance requirements
- Zero-downtime requirement during business hours
- Designed for horizontal scalability rather than fixed sizing

Exact sizing would be determined during implementation based on observed workloads, performance testing, and compliance requirements. This design prioritises scalability over fixed capacity to support variable trading demand.

## 2.6 Storage and Messaging Services

### Azure Blob Storage for Unstructured Trade Data

In this Trade Capture System, I use Azure Blob Storage for unstructured data that is generated as part of trading and settlement workflows but does not belong in the core relational trade model. Examples include settlement instruction exports, regulatory reports, audit files, and document attachments linked to trades.

These data types differ from core trade records because they do not require relational structure, foreign keys, or transactional guarantees. Storing them in the primary relational database would increase complexity and cost without providing additional value for query or integrity requirements.

By using Azure Blob Storage, I separate large, unstructured artefacts from the trade and settlement tables stored in Azure SQL Database. This allows the Trade Capture API to keep its relational database focused on high-value transactional data, while still providing durable, scalable storage for operational and compliance-driven files.

For this project, Azure Blob Storage is the most appropriate choice for handling settlement and reporting artefacts because it aligns with how these files are accessed, retained, and audited in a trading environment.

### Messaging and Asynchronous Processing Considerations

I considered Azure Queue Storage for asynchronous messaging within the Trade Capture System. Queue Storage is suitable for simple background processing scenarios where tasks can be handled independently and eventual consistency is acceptable.

In this project, trade validation, booking, and settlement workflows are handled synchronously within the API. Event-driven behaviour is currently implemented at the application level, and no external background worker or distributed processing layer is required. I am planning to change this to use Kafka.

As a result, Azure Queue Storage was not selected for the current architecture. It remains a viable option if the system evolves to include asynchronous settlement processing, export generation, or notification workflows.

As part of ongoing development and learning, I am evolving the architecture to introduce Kafka-based event streaming. This allows trade lifecycle events to be processed asynchronously and prepares the system for future background processing, integration with downstream systems, and more scalable event-driven workflows.

### Azure File Storage (Considered but Not Selected)

I considered Azure File Storage, which provides shared file system access and can be mounted by applications or user machines. This approach is commonly used for legacy systems or lift-and-shift workloads that rely on shared network drives.

For the Trade Capture System, files such as settlement exports and regulatory reports are generated and consumed programmatically through the API rather than accessed through a shared file system. Introducing Azure File Storage would increase operational complexity without aligning with how the system produces and uses these artefacts.

For this reason, Azure File Storage was not selected for the current architecture.

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

## How Kafka fits with Azure-hosted trading app (section not complete)

[ React UI ]
|
[ Trade Capture API (App Service / AKS / VM) ]
|
[ Kafka Cluster ]
|
[ Risk / Settlement / Reporting Systems ]

Produces events such as :

- TradeCreated
- TradeAmended
- SettlementInstructionsUpdated

Listen for:

- settlement confirmations

- risk alerts

-menrichment events

### Why Kafka?

In a larger enterprise trading environment, event streaming platforms such as Kafka are commonly used to distribute trade lifecycle events to downstream systems such as risk, settlement, and reporting. The Trade Capture API would act as a Kafka producer, publishing events after persisting trade state in the relational database.

Kafka is treated as an external event streaming platform and is not hosted within Azure App Service itself. It integrates with the Azure-hosted application through secure network connectivity and is used to decouple trading workflows rather than as a system of record.

When a trader books a trade, the Trade Capture API persists the trade in the relational database and then publishes a TradeCreated event to Kafka. Downstream systems such as risk, settlement, and reporting consume this event independently. Kafka retains the event for a configured period to allow replay, but it is not used as the system of record.

### Trade Lifecycle Event Streaming with Kafka

In this project, I use Kafka to model trade lifecycle event streaming in a development environment. After the Trade Capture API persists trade state in the relational database, it publishes events such as TradeCreated, TradeAmended, and SettlementInstructionsUpdated. These events represent business facts that downstream systems could consume independently.

Kafka is run locally using Docker to support hands-on learning and development. This setup mirrors how the Trade Capture API would interact with an enterprise Kafka platform, without requiring cloud-managed Kafka infrastructure. The application code produces and consumes Kafka events in the same way it would against a managed or on-premises Kafka cluster.

I run Kafka locally using Docker for development and learning. My Azure-hosted Trade Capture API produces and consumes Kafka events exactly as it would against a managed or enterprise Kafka cluster.

### Options Considered for Kafka Deployment

#### Kafka on Azure Virtual Machines

I considered running Kafka on Azure Virtual Machines, which is a common approach in large enterprises. In this model, Kafka brokers run on Linux VMs within a Virtual Network, with persistent disks and controlled network access. This approach provides full control over configuration and security.

For this project, I did not choose this option because Kafka requires multiple brokers and additional operational management, which would significantly increase cost and complexity without adding learning value for the core trade capture use case.

#### Kafka on Azure Kubernetes Service (AKS)

I also considered deploying Kafka on Azure Kubernetes Service using operators such as Strimzi. This approach is typically used by platform engineering teams to run Kafka at scale with automated orchestration.

I did not select this option because running Kafka on Kubernetes introduces substantial platform complexity and operational overhead. For a trade capture application focused on demonstrating event-driven design rather than platform engineering, this approach would be disproportionate.

#### Managed Kafka Services

Managed Kafka services, such as Confluent Cloud, were also considered. These services remove operational responsibility and provide a fully managed Kafka platform.

This option was not selected due to cost considerations and because the primary objective of this project is to demonstrate understanding of Kafka integration and event-driven architecture rather than managed service consumption.

## Disaster Recovery and Business Continuity

The Trade Capture System is designed with resilience and recovery in mind to support continuous trading operations. Managed Azure services such as Azure App Service and Azure SQL Database provide built-in redundancy, automated backups, and high availability within a region.

In the event of a failure, recovery procedures would prioritise restoring API availability and data integrity to minimise trader disruption. Recovery objectives would be defined during implementation based on regulatory requirements and business impact analysis.

# Section 5: Cost Management, Governance, and Compliance

This section describes how I apply Azure cost management and governance principles to the Trade Capture System in a banking context. The focus is on cost visibility, controlled usage, security, and compliance rather than exact pricing or deployment sizing. All decisions are aligned with enterprise practices commonly found in large financial institutions.

## 5.1 Cost Management Strategy for the Trade Capture System

The Trade Capture System is designed using a consumption-based cloud model. Azure services are selected to scale horizontally based on demand rather than relying on fixed capacity. This approach supports variable trading volumes, particularly during market open and close windows, while avoiding unnecessary idle infrastructure costs.

I prioritise Platform as a Service offerings such as Azure App Service and Azure SQL Database because they reduce operational overhead and include built-in availability and scaling. This shifts cost management away from infrastructure maintenance and toward actual application usage, which is more predictable and easier to control in a trading environment.

Azure Cost Management is used conceptually to monitor resource consumption, track spending trends, and set budgets. Alerts can be configured to notify teams when costs exceed expected thresholds, helping prevent unexpected overspend during periods of increased trading activity or development experimentation.

## 5.2 Resource Organisation and Governance Model

To support enterprise governance, Azure resources are organised using subscriptions and resource groups. Separate environments such as development, test, and production would be isolated at subscription or resource group level to reduce risk and enforce separation of concerns.

Resource groups are structured around the Trade Capture System components, including application services, data stores, and monitoring resources. This organisation supports lifecycle management, access control, and cost attribution at the system level.

Tags are applied to all resources to identify environment, application name, and ownership. This enables cost tracking by system and simplifies reporting for finance and governance teams.

## 5.3 Access Control and Identity Management

Access to Azure resources is controlled using Role-Based Access Control. Permissions are assigned based on job function rather than individual users, aligning with least-privilege principles commonly required in banking environments.

For example, developers may have permissions to deploy application code and view logs, while operations teams manage infrastructure configuration and monitoring. Sensitive resources such as databases and secrets are restricted to a minimal set of trusted roles.

Azure Active Directory provides centralised identity management and authentication, ensuring that access to cloud resources aligns with enterprise identity standards.

## 5.4 Policy Enforcement and Protection Controls

Azure Policy is used to enforce organisational standards across the Trade Capture System. Policies can restrict which resource types are allowed, enforce security configurations, and prevent the deployment of non-compliant services.

Resource locks can be applied to critical production resources such as databases to prevent accidental deletion or modification. This protects core trading data and supports operational stability.

Azure Key Vault is used conceptually to manage secrets, connection strings, and certificates. This ensures sensitive information is not stored directly in application code or configuration files.

## 5.5 Security, Privacy, and Compliance Considerations

The Trade Capture System handles sensitive financial data and must comply with strict security and privacy requirements. Azure’s shared responsibility model is applied, where Microsoft secures the underlying platform and I am responsible for securing application logic, access controls, and data usage.

Microsoft Defender for Cloud provides visibility into security posture and configuration risks. This supports proactive identification of vulnerabilities and misconfigurations that could impact traders or downstream settlement processes.

Data retention and access patterns are designed to align with financial services compliance requirements, ensuring that trade and settlement data remains protected and auditable.

## 5.6 Availability, SLAs, and Service Lifecycle Awareness

Azure services used in the Trade Capture System provide defined Service Level Agreements that support high availability and reliability expectations in trading systems. These SLAs inform architectural decisions and guide the selection of managed services.

The design acknowledges the Azure service lifecycle, ensuring that only supported and stable services are used for core trading functionality. This reduces long-term risk and supports maintainability as the platform evolves.

Overall, this governance and cost management approach ensures that the Trade Capture System remains scalable, secure, compliant, and financially controlled while supporting the operational demands of a banking trading environment.
