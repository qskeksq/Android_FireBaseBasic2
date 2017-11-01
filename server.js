var http = require('http');
var httpUrlConnection = require('request');
// fcm 설정 : 구글에서 미리 설정해 둔 서버
const fcmServerUrl = 'https://fcm.googleapis.com/fcm/send'; 
// 서버키
const serverKey = 'AAAAo5pdjEc:APA91bHZcE4vDXO1qSBhknzFGuKEVz0grnuhWF0n_o9fNUcrxr_EQM1QAdRvAfHsiGbIjPjH9fdsPoLqvBrnWDqmJGL2SJ3nN68hpeiTYth0o75KmDkRt5hiLO3ipWMQD9XBkTuMfkeX';
// 메시지
var msg = {
    to : '',
    notification : {
        title : '노티바 제목',
        body : '글자'
    }
}

var server = http.createServer((request, response)=>{
    // post 메시지 수신
    if(request.url == '/sendNotification'){
        var postData = '';
        request.on('data', (data)=>{
            postData += data;
        });
        // 메시지 수신 완료
        request.on('end', ()=>{
            // jso 스트림을 객체로 변환
            var postObj = JSON.parse(postData);
            msg.to = postObj.to;
            msg.notification.body = postObj.msg;
            httpUrlConnection(
                // http 메시지 객체
                {
                    url : fcmServerUrl,
                    method : 'POST',
                    headers : {
                        "Authorization" : "key="+serverKey,
                        'Content-Type' : 'application/json'
                    },
                    body : JSON.stringify(msg)
                },
                // 콜백 함수
                (err, answer, body)=>{
                    var result = {
                        code : answer.statusCode,
                        msg : body
                    };
                    response.end(JSON.stringify(result));
                }
            );
        });
    } else {
        response.end('404 page not found');
    }
});

server.listen(8090, ()=>{
    console.log('server is running');
});