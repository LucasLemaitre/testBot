# Remote meetings planning

This project is used in a course on the *ops* part at the [University of Rennes](https://www.univ-rennes1.fr/), France. It is a kind of doodle clone developed in so-called "cloud-native" technologies in order to allow students to work on a continuous deployment chain in a containerized environment. Among the feature, the application automatically initializes a pad for the meeting and a chat room for the meeting participants.

- The [back](https://github.com/barais/doodlestudent/tree/main/api) is developed using the [quarkus.io](https://quarkus.io/) framework. 
- The [front](https://github.com/barais/doodlestudent/tree/main/front) is developed in [angular](https://angular.io/) using the [primeng](https://www.primefaces.org/primeng/)  angular UI component library and the [fullcalendar](https://fullcalendar.io/) graphical component.

A demo of the application is available [here](https://doodle.diverse-team.fr/).

Three videos (in french) are available. They present:
- the [main application feature](https://drive.google.com/file/d/1GQbdgq2CHcddTlcoHqM5Zc8Dw5o_eeLg/preview), 
- its [architecture](https://drive.google.com/file/d/1l5UAsU5_q-oshwEW6edZ4UvQjN3-tzwi/preview) 
- and a [short code review](https://drive.google.com/file/d/1jxYNfJdtd4r_pDbOthra360ei8Z17tX_/preview) .

For french native speaker that wants to follow the course. The course web page is available [here](https://hackmd.diverse-team.fr/s/SJqu5DjSD).

## Requirments

Verify that these are installed on your computer :

- Java (JDK) 11+, e.g. [Oracle JSE](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) (with the JAVA_HOME environment variable correctly set)
- [Maven](http://maven.apache.org/install.html)
- [Git](https://git-scm.com/download/)
- [Docker](https://docs.docker.com/engine/install/) (at least version 19.03.0, 20.10 preferred)
- Docker compose ([Compose V2](https://docs.docker.com/compose/cli-command/#installing-compose-v2) preferred, should be able to run 3.8 compose files)
- [Node](https://nodejs.org/en/) at least version 16
- npm at least version 8 (installed with Node)
- A Java IDE (Eclipse, IntelliJ IDEA, NetBeans, VS Code, Xcode, etc.)

If you are on Windows, Docker can not mount files outside your user folder (Unless an absolute path is provided).
Please, clone the doodle in the user folder or change the compose file to correctly mount the etherpad APIKEY.txt
