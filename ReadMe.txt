
**************************************************************************** Url Shortener Application ***************************************************************************************

Note: This project has been created using Java.

* Prerequisites:
	- Postman to call url shortener API
	- Port number 8080 on the machine should not already be in use.

* Steps to run application using an IDE:
	- Other Prerequisites:
		- Java JDK
		- An IDE like Spring Tool Suite 4

	- Clone the source code using git and open the "Url-Shortener/app" folder in the IDE.
	- Start the application. (by default, application starts on 8080 port number). 
	- Make API (POST request with long URL) calls from Postman.

* Steps to run application using Docker:
	- Other Prerequisites:
		- Docker

	- Clone the source code using git.
	- Go inside the "Url-Shortener" directory. 
	- Build the docker image using below command:
		docker build -t url-shortener-image:latest .
	- Run the docker container using below command:
		docker run -itd -p 8080:8080 --name urlShortenerContainer url-shortener-image:latest
	- Make API (POST request with long URL) calls from Postman.