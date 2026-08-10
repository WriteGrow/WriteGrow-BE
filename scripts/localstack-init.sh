#!/bin/bash
# LocalStack 기동 완료 후 개발용 S3 버킷을 생성한다.
awslocal s3 mb s3://writegrow-dev
awslocal s3api put-bucket-cors --bucket writegrow-dev --cors-configuration '{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST"],
      "AllowedOrigins": ["*"],
      "MaxAgeSeconds": 3000
    }
  ]
}'
