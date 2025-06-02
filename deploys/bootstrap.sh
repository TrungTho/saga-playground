#!/bin/sh
set -e
set -m

#############################
#### kafka bootstrapping ####
#############################

HOST=kafka-1:29092
NUMBER_PARTITION=4
NUMBER_RF=2
CHECKOUT_SUCCESSFUL=db.saga_playground.checkout.successful
CHECKOUT_FAILED=db.saga_playground.checkout.failed
FULFILLMENT_SUCCESSFUL=db.saga_playground.fulfillment.successful
FULFILLMENT_FAILED=db.saga_playground.fulfillment.failed

# echo "=====================";
# echo "=====Start Kafka=====";
# echo "=====================";
# /etc/confluent/docker/run & KAFKA_PID=$! # run kafka in background in order not to block the below bootstrap steps

sleep 5s; # to make sure kafka server is ready to handle CLI request
echo "=====================";
echo "=====Init topics=====";
echo "=====================";

# check if topics exist -> create topics if needed

# CHECKOUT_SUCCESSFUL topic
if ! kafka-topics --bootstrap-server $HOST --describe --topic $CHECKOUT_SUCCESSFUL &> /dev/null; then
    echo "topic $CHECKOUT_SUCCESSFUL does not exist";
    kafka-topics --bootstrap-server $HOST \
        --create --topic $CHECKOUT_SUCCESSFUL \
        --replication-factor $NUMBER_RF \
        --partitions $NUMBER_PARTITION;
    echo "topic $CHECKOUT_SUCCESSFUL was successfully created";
else
    echo "topic $CHECKOUT_SUCCESSFUL already existed";
fi;

# CHECKOUT_FAILED topic
if ! kafka-topics --bootstrap-server $HOST --describe --topic $CHECKOUT_FAILED &> /dev/null; then
    echo "topic $CHECKOUT_FAILED does not exist";
    kafka-topics --bootstrap-server $HOST \
        --create --topic $CHECKOUT_FAILED \
        --replication-factor $NUMBER_RF \
        --partitions $NUMBER_PARTITION;
    echo "topic $CHECKOUT_FAILED was successfully created";
else
    echo "topic $CHECKOUT_FAILED already existed";
fi;
    
# FULFILLMENT_SUCCESSFUL topic
if ! kafka-topics --bootstrap-server $HOST --describe --topic $FULFILLMENT_SUCCESSFUL &> /dev/null; then
    echo "topic $FULFILLMENT_SUCCESSFUL does not exist";
    kafka-topics --bootstrap-server $HOST \
        --create --topic $FULFILLMENT_SUCCESSFUL \
        --replication-factor $NUMBER_RF \
        --partitions $NUMBER_PARTITION;
    echo "topic $FULFILLMENT_SUCCESSFUL was successfully created";
else
    echo "topic $FULFILLMENT_SUCCESSFUL already existed";
fi;

# FULFILLMENT_FAILED topic
if ! kafka-topics --bootstrap-server $HOST --describe --topic $FULFILLMENT_FAILED &> /dev/null; then
    echo "topic $FULFILLMENT_FAILED does not exist";
    kafka-topics --bootstrap-server $HOST \
        --create --topic $FULFILLMENT_FAILED \
        --replication-factor $NUMBER_RF \
        --partitions $NUMBER_PARTITION;
    echo "topic $FULFILLMENT_FAILED was successfully created";
else
    echo "topic $FULFILLMENT_FAILED already existed";
fi;

echo "==========================";
echo "Finish Kafka Bootstrapping";
echo "==========================";

# # bring the background running Kafka to Foreground to prevent container stop
# echo "=====================";
# echo "=======FG Kafka======";
# echo "=====================";
# echo "Wait for PID" $KAFKA_PID;
# wait $KAFKA_PID
# echo "Exit status: $?"
