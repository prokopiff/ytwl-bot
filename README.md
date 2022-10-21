# ytwl-bot

Bot that sends you your YouTube subscriptions in Telegram and allows to add to two different playlists with one tap.

## Docker compose example
```yaml
version: "3.8"

services:
  ytbot:
    container_name: ytbot
    image: vprokopiv/ytbot:amd64 # or :arm64
    environment:
      - "BOT_TOKEN=<telegram bot token>"
      - "BOT_CHAT_ID=<id of a chat with yourself>"
      - "BOT_WL_PLAYLIST_ID=<youtube playlist id to watch later>"
      - "BOT_LL_PLAYLIST_ID=<youtube playlist id to listen later>"
      - "SECRETS_CLIENT_ID=<google api client id>"
      - "SECRETS_CLIENT_SECRET=<google api secret>"
      - "SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/ytbot"
      - "SPRING_DATASOURCE_USERNAME=<db username>"
      - "SPRING_DATASOURCE_PASSWORD=<db password>"
    volumes:
      - '/home/pi/docker-data/ytbot/logs/:/logs/'
    restart: unless-stopped
```

## Manual Installation
Fill empty values in `app.properties`

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
