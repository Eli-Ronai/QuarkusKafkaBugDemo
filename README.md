This is a demo of the Quarkus Kafka bug.

Steps to reproduce:

1. mvn build

2. RestartDocker.bat (or docker-compose down, docker-compose up)

3. Run test

If you run the test a second time you can see that it passes. In order to reproduce the bug the docker container must be destroyed, not stopped.
