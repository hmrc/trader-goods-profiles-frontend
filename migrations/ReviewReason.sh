#!/bin/bash

echo ""
echo "Applying migration ReviewReason"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /reviewReason                       controllers.ReviewReasonController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "reviewReason.title = reviewReason" >> ../conf/messages.en
echo "reviewReason.heading = reviewReason" >> ../conf/messages.en

echo "Migration ReviewReason completed"
