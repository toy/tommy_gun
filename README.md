# Tommy Gun

Multi armed bandit using Thompson Sampling. 
Implemented on Spark Streaming, reading from Apache Kafka.  


A multi armed Bandit is a simple reinforcement learning algorithm. 
It plays on multiple slot machines and optimizes the time spent on machines with best reward.  

## Algorithm

Thompson Sampling is using the Beta Distribution. The distribution has two weights α and β. 
When pulling arms, β is increased for the arm. On reward, α is increased for the arm. Finally 
a score is computed for the arm, by drawing from the Beta distribution with the respective weight.
Higher α and lower β lead to higher scores. The player would always choose the arm with best score.  

https://towardsdatascience.com/how-not-to-sort-by-popularity-92745397a7ae

## Implementation

Given a website wants to optimize which color to use for an action button.
It would ask a rest service with access to an aggregated choices dataset, which 
colour to show at any given moment.  

Afterwards following procedure is applied:    

1. Send message via Kafka queue, which color was shown to user.
2. Observe reward - user clicking the button
3. Send reward message with chosen colour.
4. Spark streaming job receives draw and reward messages.
5. Messages are mapped to arm choices with increased/decreasd alpha/beta weight
6. Arms alpha and betas are reduced to scores dataset according to beta distribution

## Run with docker

Run `docker-compose run --rm --service-ports spark`.

In the `spark` container terminal run:

```bash
KAFKA_BROKERS=kafka:9092 \
KAFKA_GROUP_ID=tommy_gun \
KAFKA_TOPIC=events \
/usr/spark-2.2.0/bin/spark-submit \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.2.0 \
  --class dev.dre_hh.tommy_gun.Job build/tommy_gun.jar
```

In a separate terminal run

```bash
docker exec -it $(docker-compose ps -q kafka) kafka-console-producer.sh --broker-list localhost:9092 --topic events
```

And add JSON messages like:

```bash
{"action":"draw","issue":"colors", "arm":"red"}
{"action":"reward","issue":"colors", "arm":"green"}
```

The Result is a table of arms with respective weight score
```text
+------------+------------------------------------------+
|value       |ReduceAggregator(dev.dre_hh.tommy_gun.Arm)|
+------------+------------------------------------------+
|colors_red  |[colors,red,1,1,0.9992956407034402]       |
|colors_green|[colors,green,5,1,0.9356279293450386]     |
+------------+------------------------------------------+
```

## Compile from source
```bash
# build fat jar
sbt asembly
```
