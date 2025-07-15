#!/usr/bin/env bash
set -euo pipefail

echo "Creating Accounts table..."
awslocal dynamodb create-table \
  --table-name Accounts \
  --attribute-definitions AttributeName=U,AttributeType=B \
  --key-schema AttributeName=U,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# Missing AppleDeviceChecks
# Missing AppleDeviceCheckPublicKeys
# Missing Backups 

echo "Creating ClientReleases table..."
awslocal dynamodb create-table \
  --table-name ClientReleases \
  --attribute-definitions \
      AttributeName=P,AttributeType=S \
      AttributeName=V,AttributeType=S \
  --key-schema \
      AttributeName=P,KeyType=HASH \
      AttributeName=V,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST
echo "Setting TTL on ClientReleases (attribute E)..."
awslocal dynamodb update-time-to-live \
  --table-name ClientReleases \
  --time-to-live-specification \
      "Enabled=true,AttributeName=E"

echo "Creating DeleteAccounts table..."
awslocal dynamodb create-table \
  --table-name DeleteAccounts \
  --attribute-definitions AttributeName=P,AttributeType=S \
  --key-schema AttributeName=P,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

echo "Creating DeleteAccountsLock table..."
awslocal dynamodb create-table \
  --table-name DeleteAccountsLock \
  --attribute-definitions AttributeName=P,AttributeType=S \
  --key-schema AttributeName=P,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# Missing IssuedReceipts

echo "Creating Keys table..."
awslocal dynamodb create-table \
  --table-name Keys \
  --attribute-definitions \
      AttributeName=U,AttributeType=B \
      AttributeName=DK,AttributeType=B \
  --key-schema \
      AttributeName=U,KeyType=HASH \
      AttributeName=DK,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST

# Missing EC_Signed_Pre_Keys
# Missing PQ_Keys
# Missing PQ_Paged_Keys
# Missing PQ_Last_Resort_Keys

echo "Creating Messages table..."
awslocal dynamodb create-table \
  --table-name Messages \
  --attribute-definitions \
      AttributeName=H,AttributeType=B \
      AttributeName=S,AttributeType=B \
      AttributeName=U,AttributeType=B \
  --key-schema \
      AttributeName=H,KeyType=HASH \
      AttributeName=S,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --local-secondary-indexes \
      "[{
         \"IndexName\": \"Message_UUID_Index\",
         \"KeySchema\": [
             {\"AttributeName\": \"H\",\"KeyType\": \"HASH\"},
             {\"AttributeName\": \"U\",\"KeyType\": \"RANGE\"}
          ],
         \"Projection\": {\"ProjectionType\": \"KEYS_ONLY\"}
       }]"

# Missing OnetimeDonations
# Missing PhoneNumberIdentifiers
# Missing Profiles

echo "Creating PushChallenges table..."
awslocal dynamodb create-table \
  --table-name PushChallenges \
  --attribute-definitions AttributeName=U,AttributeType=B \
  --key-schema AttributeName=U,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
echo "Setting TTL on PushChallenges (attribute T)..."
awslocal dynamodb update-time-to-live \
  --table-name PushChallenges \
  --time-to-live-specification \
      "Enabled=true,AttributeName=T"

# Missing PushNotificationExperimentSamples 
# Missing RedeemedReceipts
# Missing RegistrationRecovery
# Missing RemoteConfig
  
echo "Creating ReportMessages table..."
awslocal dynamodb create-table \
  --table-name ReportMessages \
  --attribute-definitions AttributeName=H,AttributeType=B \
  --key-schema AttributeName=H,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
echo "Setting TTL on ReportMessages (attribute E)..."
awslocal dynamodb update-time-to-live \
  --table-name ReportMessages \
  --time-to-live-specification \
      "Enabled=true,AttributeName=E"

# Missing ScheduledJobs
# Missing Subscriptions
# Missing ClientPublicKeys
# Missing VerificationSessions

echo "All tables created!"
