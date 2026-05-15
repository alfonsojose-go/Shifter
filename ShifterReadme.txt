src/
├── main/
│   ├── java/
│   │   └── com/example/project/
│   │       ├── ProjectApplication.java   # Main application entry point
│   │       ├── controller/             # Handles web requests (MVC Controller)
│   │       ├── service/                # Contains business logic
│   │       ├── repository/             # Handles data access (DAO/Repository)
│   │       ├── model/                  # Domain entities or POJOs
│   │       ├── config/                 # Spring configuration classes
│   │       └── util/                   # Utility classes
│   └── resources/
│       ├── application.properties      # Main configuration file
│       ├── static/                     # Static files (CSS, JS, images)
│       ├── templates/                  # Server-side templates (e.g., Thymeleaf)
│       └── ...
└── test/
    └── java/
        └── com/example/project/        # Test classes mirroring 'main/java' structure
            ├── controller/TestController.java
            ├── service/TestService.java
            └── ...