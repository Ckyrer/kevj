function send (req, func) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', req, true);
    xhr.send();
    xhr.onreadystatechange = () => {
        if (xhr.readyState != 4) return;
        func(xhr)
    }
}

const username = document.querySelector('.name')
const password = document.querySelector('.password')

function login() {
    send('CMD<>login<>'+ username.value+'<>'+password.value, (resp) => {
        if (resp.responseText == 1) {
            window.location.replace("/")
        } else if (resp.responseText == 0) {
            alert('Неверный логин или пароль!')
        } else {
            alert('Неизвестная ошибка!')
        }
    })
}
