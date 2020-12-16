curl -X PUT "localhost:9200/_template/template_userinfo?pretty" -H 'Content-Type: application/json' -d'
{"index_patterns":["userinfo*"],"settings":{"number_of_shards":1},"mappings":{"_source":{"enabled":true},"properties":{"username":{"type":"keyword"},"password":{"type":"keyword"},"accountCreationDate":{"type":"date","format":"yyyy-MM-dd HH:mm:ss"},"employee":{"type":"boolean"},"affiliate":{"type":"boolean"},"customer":{"type":"boolean"}}}}
'

curl -X PUT "localhost:9200/_template/template_product?pretty" -H 'Content-Type: application/json' -d'
{"index_patterns":["product*"],"settings":{"number_of_shards":1},"mappings":{"_source":{"enabled":true},"properties":{"productName":{"type":"keyword"},"type":{"type":"keyword"},"price":{"type":"long"}}}}
'
