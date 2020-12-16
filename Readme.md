### Retail Discount Calculator
This is a retail discount calculator api project. It uses elastic search to save and search necessary user and project informations.
In elasticsearch 2 indices are stored. One of them is the "userinfo" index and the other one is the "product" index. It uses Spring Boot framework. 3 api endpoints are created for the project

# Api information
- /user (POST): This api is used to create user information in the elasticsearch.
- /products/{type} (GET): This api is used to get products of certain types.
- /calculatebasket (POST): This api is used to calculate total prices of the products in the basket according to the given user detail information inside of the request body.

user and products/{type} endpoints are not used to calculate discounted price of the products in the basket. They were created thinking the website operations.

# Sample product document from product index
```json
{
  "productName": "Blue Dress",
  "type": "garment",
  "price": 400
}
```

# Sample userinfo document from userinfo index
```json
{
  "username": "example@mail.com",
  "password": "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
  "employee": true,
  "affiliate": true,
  "customer": true,
  "accountCreationDate": "2020-12-16 09:30:11"
}
```


# elastic docker

```
docker run --name personaltestelastic -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.7.0
```

# run scripts
Under the project, there is a settings folder which has elastic search sh files to create indices and save product information. 

Before starting the service, user must go to that folder and execute the following commands.
# execute template sh
```
chmod +x template.sh
./template.sh

```

# execute index sh
```
chmod +x index.sh
./index.sh

```

# execute products sh
```
chmod +x products.sh
./products.sh

```

# Api Operation Information
## user api body
```json
{
    "username": "example@mail.com",
    "password": "123456",
    "employee": true,
    "affiliate": true,
    "customer": true
}
```

## calculatebasket api body
```json
{
    "userDetails": {
        "username": "example@mail.com",
        "employee": true,
        "affiliate": true,
        "customer": true,
        "accountCreationDate": "2020-12-16 00:12:47"
    },
    "basketDetails": [
        {
            "productName": "Red Carpet",
            "amount": 1
        },
        {
            "productName": "Blue Dress",
            "amount": 2
        },
        {
            "productName": "Bananas",
            "amount": 1
        }
    ]
}
```

# Run project
## open the project root and execute the following command
```
mvn spring-boot:run
```
## in order to show the test covarage with jacoco and then go to the target/site/jacoco/index.html
```
mvn clean test
```
