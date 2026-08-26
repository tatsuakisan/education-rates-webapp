# Education Rates Web Application

This project is a web application developed using Java and Spring Boot, designed to display and visualize data on high school attendance rates and graduation rates. The application features an interactive dashboard that presents this data through graphs and charts.

## Features

- Interactive dashboard for visualizing attendance and graduation rates.
- Data retrieval and processing using a service layer.
- Responsive design with CSS for a user-friendly interface.
- JavaScript integration for dynamic chart rendering.

## Project Structure

```
education-rates-webapp
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── educationrates
│   │   │               ├── EducationRatesApplication.java
│   │   │               ├── controller
│   │   │               │   └── DashboardController.java
│   │   │               ├── model
│   │   │               │   └── RateRecord.java
│   │   │               └── service
│   │   │                   └── RateService.java
│   │   └── resources
│   │       ├── static
│   │       │   ├── css
│   │       │   │   └── styles.css
│   │       │   └── js
│   │       │       └── charts.js
│   │       ├── templates
│   │       │   └── index.html
│   │       └── application.properties
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── educationrates
│                       └── EducationRatesApplicationTests.java
├── pom.xml
└── README.md
```

## Setup Instructions

1. **Clone the repository:**
   ```
   git clone https://github.com/yourusername/education-rates-webapp.git
   cd education-rates-webapp
   ```

2. **Build the project:**
   Ensure you have Maven installed, then run:
   ```
   mvn clean install
   ```

3. **Run the application:**
   Use the following command to start the Spring Boot application:
   ```
   mvn spring-boot:run
   ```

4. **Access the application:**
   Open your web browser and navigate to `http://localhost:8080` to view the dashboard.

## Usage

The dashboard will display graphs representing the rates of high school attendance and graduation. You can interact with the charts to gain insights into the data trends.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.