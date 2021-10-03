# ytwl-bot

Bot that sends you your YouTube subscriptions in Telegram and allows to add to a playlist.

## Installation
Fill empty values in `app.properties`, `client_secrets.json`

```shell
mvn package
```

```shell
java -jar target/ytbot.jar
```

### Service configuration example
```shell
[Unit]
Description = YouTube Watch Later Telegram Bot
After=syslog.target network.target

[Service]
Type=simple
WorkingDirectory=/home/pi
ExecStart=/home/pi/jdk-17+35/bin/java -jar /home/pi/ytbot.jar
ExecStop=/bin/kill -15 $MAINPID

[Install]
WantedBy=multi-user.target
```
