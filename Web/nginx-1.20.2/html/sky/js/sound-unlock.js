(function () {
  'use strict';

  var probeAudio = null;
  var actualAudioReady = false;
  var NativeWebSocket = window.WebSocket;

  function ReconnectingWebSocket(url, protocols) {
    this.url = url;
    this.readyState = 0;
    this.onopen = null;
    this.onmessage = null;
    this.onerror = null;
    this.onclose = null;
    this._protocols = protocols;
    this._closedByUser = false;
    this._errorReported = false;
    this._connect();
  }

  ReconnectingWebSocket.CONNECTING = 0;
  ReconnectingWebSocket.OPEN = 1;
  ReconnectingWebSocket.CLOSING = 2;
  ReconnectingWebSocket.CLOSED = 3;

  ReconnectingWebSocket.prototype._connect = function () {
    var that = this;
    this.readyState = ReconnectingWebSocket.CONNECTING;
    this._socket = this._protocols
      ? new NativeWebSocket(this.url, this._protocols)
      : new NativeWebSocket(this.url);

    this._socket.onopen = function (event) {
      that.readyState = ReconnectingWebSocket.OPEN;
      that._errorReported = false;
      console.info('[苍穹外卖] 管理员 WebSocket 已连接，可自动接收来单和催单');
      if (that.onopen) {
        that.onopen(event);
      }
    };

    this._socket.onmessage = function (event) {
      if (that.onmessage) {
        that.onmessage(event);
      }
    };

    this._socket.onerror = function (event) {
      if (that.onerror && !that._errorReported) {
        that._errorReported = true;
        that.onerror(event);
      }
    };

    this._socket.onclose = function (event) {
      that.readyState = ReconnectingWebSocket.CLOSED;
      if (that.onclose) {
        that.onclose(event);
      }

      if (!that._closedByUser) {
        console.warn('[苍穹外卖] WebSocket 已断开，3 秒后自动重连');
        setTimeout(function () {
          if (!that._closedByUser) {
            that._connect();
          }
        }, 3000);
      }
    };
  };

  ReconnectingWebSocket.prototype.send = function (data) {
    if (this._socket && this._socket.readyState === NativeWebSocket.OPEN) {
      this._socket.send(data);
    }
  };

  ReconnectingWebSocket.prototype.close = function (code, reason) {
    this._closedByUser = true;
    this.readyState = ReconnectingWebSocket.CLOSING;
    if (this._socket) {
      this._socket.close(code, reason);
    }
  };

  window.WebSocket = ReconnectingWebSocket;

  function findAudio(keyword) {
    var audios = Array.prototype.slice.call(document.querySelectorAll('audio'));
    return audios.find(function (audio) {
      var sources = Array.prototype.slice.call(audio.querySelectorAll('source'));
      return sources.some(function (source) {
        return source.src.indexOf(keyword) !== -1;
      });
    }) || null;
  }

  function createProbeAudio() {
    if (probeAudio) {
      return probeAudio;
    }

    probeAudio = document.createElement('audio');
    probeAudio.src = '/media/preview.3f1fe127.mp3';
    probeAudio.preload = 'auto';
    probeAudio.style.display = 'none';
    document.body.appendChild(probeAudio);
    return probeAudio;
  }

  function primeSilently(audio) {
    if (!audio) {
      return Promise.resolve();
    }

    try {
      audio.pause();
      audio.currentTime = 0;
      audio.muted = false;
      audio.volume = 0;

      var result = audio.play();
      return Promise.resolve(result).then(function () {
        audio.pause();
        audio.currentTime = 0;
        audio.volume = 1;
      });
    } catch (error) {
      return Promise.reject(error);
    }
  }

  function unlockFromUserAction() {
    if (actualAudioReady) {
      return;
    }

    var previewAudio = findAudio('preview');
    var reminderAudio = findAudio('reminder');
    var audioList = [previewAudio, reminderAudio].filter(Boolean);

    // 登录页还没有管理员布局，先用临时音频获得浏览器的播放许可。
    if (audioList.length === 0) {
      audioList = [createProbeAudio()];
    }

    Promise.all(audioList.map(primeSilently)).then(function () {
      if (previewAudio && reminderAudio) {
        actualAudioReady = true;
        console.info('[苍穹外卖] 管理员来单和催单提示音已自动解锁');
      }
    }).catch(function (error) {
      console.warn('[苍穹外卖] 提示音自动解锁暂未完成，下一次点击会继续尝试：', error);
    });
  }

  document.addEventListener('pointerdown', unlockFromUserAction, true);
  document.addEventListener('keydown', unlockFromUserAction, true);
  document.addEventListener('touchstart', unlockFromUserAction, {
    capture: true,
    passive: true
  });
})();
