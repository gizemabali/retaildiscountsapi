# userinfo index

curl -X PUT "localhost:9200/userinfo?pretty"

# product index

curl -X PUT "localhost:9200/product?pretty"

# index listing

curl -X GET "localhost:9200/_cat/indices?v"
