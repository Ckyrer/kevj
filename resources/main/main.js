function send (req, func) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', req, true);
    xhr.send();
    xhr.onreadystatechange = () => {
        if (xhr.readyState != 4) return;
        func(xhr)
    }
}

const url = document.querySelector('.url-input')
const title = document.querySelector('.name-input')
const send_button = document.querySelector('.send')

function add() {
    send_button.disabled = true
    send('CMD<>add<>'+title.value+'<>'+url.value, (xhr) => {
        if (xhr.responseText == '1') {
            document.location.reload()
        } else {
            alert('Неизвестная ошибка!')
        }
    })
}

function logout() {
    send('CMD<>logout', (xhr) => {
        document.location.reload()
    })
}
