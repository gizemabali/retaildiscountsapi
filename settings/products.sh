curl -X PUT "localhost:9200/product/_doc/1?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Blue Shoes",
  "type": "shoes",
  "price": 200
}
'
curl -X PUT "localhost:9200/product/_doc/2?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Blue Dress",
  "type": "garment",
  "price": 400
}
'

curl -X PUT "localhost:9200/product/_doc/3?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Red Carpet",
  "type": "home",
  "price": 100
}
'

curl -X PUT "localhost:9200/product/_doc/4?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Red Sofa",
  "type": "home",
  "price": 150
}
'

curl -X PUT "localhost:9200/product/_doc/5?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Bananas",
  "type": "groceries",
  "price": 15
}
'

curl -X PUT "localhost:9200/product/_doc/6?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Mango",
  "type": "groceries",
  "price": 15
}
'
curl -X PUT "localhost:9200/product/_doc/7?pretty" -H 'Content-Type: application/json' -d'
{
  "productName": "Apple",
  "type": "groceries",
  "price": 15
}
'
