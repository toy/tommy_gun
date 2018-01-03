FROM gettyimages/spark
RUN mkdir /tommy_gun
WORKDIR /tommy_gun
COPY . /tommy_gun
COPY ./conf /usr/spark-2.2.0/conf
