function send (req, func) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', req, true);
    xhr.send();
    xhr.onreadystatechange = () => {
        if (xhr.readyState != 4) return;
        func(xhr)
    }
}

function logout() {
    send('CMD<>logout', (xhr) => {
        document.location.reload()
    })
}
