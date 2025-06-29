Instructions to the Candidate for the Home Task

Create a simple application related to transaction management within a banking system.

The application should enable users to record, view, and manage financial transactions.

User authentication is not required for this task. You are expected to complete this task independently and can use any resources or reference materials you consider helpful. The completed work should be submitted via email or a public GitHub repository within 48 hours of receiving this assignment. The task should take approximately 1 to 2 hours to complete.
After reviewing the submission, we will schedule a follow-up discussion.

Instructions:
• Write in Java 21 and Spring Boot
• The primary entity is the transaction
• All data should be held in memory; no persistent storage is necessary
• Key points to address are:
• Clear and well-structured API

• Emphasis on performance for all core operations

• Comprehensive testing, including unit and stress testing

• Containerization with tools like Docker, Kubernetes

• Implement caching mechanisms where applicable

• Robust validation and exception handling

• Efficient data queries and pagination

• Follow RESTful API design principles

• Deliverable should be a stand-alone project that is straightforward to run and test
• Use Maven for project management
• If you use external libraries outside the standard JDK, list them in the README and explain their purpose
• Ensure page functionalities: adding, modifying, deleting transactions, displaying the transaction list on the page

API to Implement:
Design your functions with clear action descriptions.
• Create transaction
• Delete transaction
• Modify transaction
• List all transactions
• Implement error handling for scenarios such as creating duplicate transactions or deleting a non-existent transaction
• Perform unit testing on the API to ensure robustness and reliability
• If relevant, handle and test the logic for transaction types or categories
• Ensure the API can withstand stress tests and maintain performance under load