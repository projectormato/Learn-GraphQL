# Learn-GraphQL
[How to GraphQL](https://www.howtographql.com/)をやりながらメモするリポジトリ


## Introduction
[Introductionのページ](https://www.howtographql.com/)  
GraphQL is a new API standard that provides a more efficient, powerful and flexible alternative to REST.  
GraphQLはRESTに代わる新しいすごいやつ！とFacebookは言っている。

a GraphQL server only exposes a single endpoint and responds with precisely the data a client asked for.  
GraphQLは単一のエンドポイントだけでデータを返す。

### RESTの問題点とGraphQLの利点
1. Increased mobile usage creates need for efficient data loading  
GraphQLは良くないネットワークでもデータを最小限に抑えて動作を大幅に改善できる

2. Variety of different frontend frameworks and platforms  
エンドポイントが単一なので、(クライアントを実行する)どんなフレームワークやプラットフォームからでも問題がない(それぞれエンドポイント作らなくていい)

3. Fast development & expectation for rapid feature development  
RESTではクライアントの設計の変更とかで作りなおしたり、それを考慮して作らないといけないから、それが開発の速度を落としたりしている(?)

### History, Context & Adoption
GraphQLはReactのためのものじゃないよ！ネイティブアプリでも使ってよ！！みたいなことが書いてある

## GraphQL is the better REST
[ページ](https://www.howtographql.com/basics/1-graphql-is-the-better-rest/)
RESTとの違いを示すために、ブログアプリケーションで特定のユーザの記事のタイトルと、同じ画面にそのユーザーの最新のフォロワーを表示する例が出されてる。  

### Data Fetching with REST vs GraphQL
そのためにRESTでは、ユーザの名前を取るために
``
/users/<id>
``
、(全ての)投稿を取るために
``
/users/<id>/posts
``
、(全ての)フォロワーを取るために
``
/users/<id>/followers
``
の計3回のフェッチをします。

しかしGraphQLでは
```
query{
  User(id: "5nioapif") {
    name
    posts{
      title
    }
    followers(last: 3){
      name
    }
  }
}
```
みたいなフェッチを1回すれば十分だ、ということらしい。


### No more Over- and Underfetching
* オーバーフェッチ・・・``/users``にフェッチした時に名前だけ欲しい時も住所とか誕生日とかいらん情報もふってくる
* アンダーフェッチ・・・全ユーザの最新フォロワー3人が欲しい時、``/users``にフェッチしてから書くユーザの``/users/<user-id>/followers``にフェッチしなきゃだよね、みたいな。特定のエンドポイントが必要な情報を十分に提供しないこと

が、No moreらしい

### Rapid Product Iterations on the Frontend
クライアント側の仕様が変更になって、必要なデータが変わっても(減っても増えても)問題ないよー、GraphQLならね。ということが書いてある気がする。

バックエンド側で分析したりボトルネック探したりするの便利、  
スキーマ定義しておくとモックサーバみたいなのをそれで立てればサーバ側が準備出来てなくてもクライアント側でテスト出来るから良い感じ。
