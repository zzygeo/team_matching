FROM openjdk:11

COPY team_yupao.jar /home/

RUN chmod +x /home/team_yupao.jar

CMD ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/home/team_yupao.jar"]