const endpointInput = document.querySelector("#endpointInput");
const conversationInput = document.querySelector("#conversationInput");
const healthBtn = document.querySelector("#healthBtn");
const loadBtn = document.querySelector("#loadBtn");
const newBtn = document.querySelector("#newBtn");
const clearBtn = document.querySelector("#clearBtn");
const smoothToggle = document.querySelector("#smoothToggle");
const statusText = document.querySelector("#statusText");
const elapsedText = document.querySelector("#elapsedText");
const chunkText = document.querySelector("#chunkText");
const charText = document.querySelector("#charText");
const summaryText = document.querySelector("#summaryText");
const providerNotice = document.querySelector("#providerNotice");
const connectionBadge = document.querySelector("#connectionBadge");
const messages = document.querySelector("#messages");
const events = document.querySelector("#events");
const chatForm = document.querySelector("#chatForm");
const messageInput = document.querySelector("#messageInput");
const sendBtn = document.querySelector("#sendBtn");
const stopBtn = document.querySelector("#stopBtn");
const clearEventsBtn = document.querySelector("#clearEventsBtn");
const promptChips = document.querySelectorAll(".prompt-chip");

const storageKey = "ai-assistant-stream-lab-conversation-id";

let abortController = null;
let activeBubble = null;
let activeMessage = null;
let startedAt = 0;
let timerId = 0;
let chunkCount = 0;
let charCount = 0;
let eventBuffer = "";
let renderQueue = [];
let renderTimer = 0;

conversationInput.value = localStorage.getItem(storageKey) || "";
refreshConfig();

chatForm.addEventListener("submit", (event) => {
    event.preventDefault();
    sendMessage();
});

messageInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && event.ctrlKey) {
        event.preventDefault();
        sendMessage();
    }
});

stopBtn.addEventListener("click", () => {
    if (abortController) {
        abortController.abort();
    }
});

healthBtn.addEventListener("click", checkBackend);
loadBtn.addEventListener("click", loadConversation);
newBtn.addEventListener("click", startNewConversation);
clearBtn.addEventListener("click", clearConversation);
clearEventsBtn.addEventListener("click", () => {
    events.replaceChildren();
});

promptChips.forEach((chip) => {
    chip.addEventListener("click", () => {
        messageInput.value = chip.textContent;
        messageInput.focus();
    });
});

function setStatus(text, state = "idle") {
    statusText.textContent = text;
    connectionBadge.textContent = state === "live" ? "Live" : state === "error" ? "Error" : "Idle";
    connectionBadge.className = `badge ${state}`;
}

function setBusy(isBusy) {
    sendBtn.disabled = isBusy;
    stopBtn.disabled = !isBusy;
    messageInput.disabled = isBusy;
}

function resetStats() {
    chunkCount = 0;
    charCount = 0;
    eventBuffer = "";
    renderQueue = [];
    clearTimeout(renderTimer);
    renderTimer = 0;
    chunkText.textContent = "0";
    charText.textContent = "0";
    elapsedText.textContent = "0.0s";
}

async function sendMessage() {
    const message = messageInput.value.trim();

    if (!message || abortController) {
        return;
    }

    removeEmptyState();
    appendMessage("user", message);
    activeMessage = appendMessage("assistant", "");
    activeMessage.classList.add("streaming");
    activeBubble = activeMessage.querySelector(".bubble");
    messageInput.value = "";
    resetStats();
    setBusy(true);
    setStatus("连接中", "live");
    startedAt = performance.now();
    timerId = window.setInterval(updateElapsed, 100);
    abortController = new AbortController();

    try {
        const payload = {
            conversationId: conversationInput.value.trim() || null,
            message
        };

        const response = await fetch(endpointInput.value.trim() || "/chat/stream", {
            method: "POST",
            headers: {
                "Accept": "text/event-stream",
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload),
            signal: abortController.signal
        });

        if (!response.ok || !response.body) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP ${response.status}`);
        }

        await readSseStream(response.body);
        await waitForRenderQueue();
        setStatus("已完成", "idle");
    } catch (error) {
        await waitForRenderQueue();

        if (error.name === "AbortError") {
            addEvent("abort", "前端已停止读取响应。");
            setStatus("已停止", "idle");
        } else {
            addEvent("error", error.message);
            setStatus("请求失败", "error");
            if (activeBubble && !activeBubble.textContent.trim()) {
                activeBubble.textContent = error.message;
            }
        }
    } finally {
        window.clearInterval(timerId);
        updateElapsed();
        setBusy(false);
        abortController = null;
        if (activeMessage) {
            activeMessage.classList.remove("streaming");
        }
        activeMessage = null;
        activeBubble = null;
        messageInput.disabled = false;
        messageInput.focus();
    }
}

async function readSseStream(body) {
    const reader = body.getReader();
    const decoder = new TextDecoder("utf-8");

    while (true) {
        const { value, done } = await reader.read();

        if (done) {
            break;
        }

        eventBuffer += decoder.decode(value, { stream: true });
        eventBuffer = eventBuffer.replace(/\r\n/g, "\n");
        consumeEvents();
    }

    eventBuffer += decoder.decode();
    eventBuffer = eventBuffer.replace(/\r\n/g, "\n");
    consumeEvents(true);
}

function consumeEvents(flush = false) {
    let separatorIndex = eventBuffer.indexOf("\n\n");

    while (separatorIndex >= 0) {
        const rawEvent = eventBuffer.slice(0, separatorIndex);
        eventBuffer = eventBuffer.slice(separatorIndex + 2);
        handleSseEvent(parseSseEvent(rawEvent));
        separatorIndex = eventBuffer.indexOf("\n\n");
    }

    if (flush && eventBuffer.trim()) {
        handleSseEvent(parseSseEvent(eventBuffer));
        eventBuffer = "";
    }
}

function parseSseEvent(rawEvent) {
    const event = {
        name: "message",
        data: ""
    };
    const dataLines = [];

    rawEvent.split("\n").forEach((line) => {
        if (!line || line.startsWith(":")) {
            return;
        }

        if (line.startsWith("event:")) {
            event.name = line.slice(6).trim() || "message";
        }

        if (line.startsWith("data:")) {
            dataLines.push(line.slice(5).replace(/^ /, ""));
        }
    });

    event.data = dataLines.join("\n");
    return event;
}

function handleSseEvent(event) {
    addEvent(event.name, event.data);

    if (event.name === "start") {
        setConversationId(event.data);
        setStatus("接收中", "live");
        return;
    }

    if (event.name === "message") {
        chunkCount += 1;
        charCount += Array.from(event.data).length;
        chunkText.textContent = String(chunkCount);
        charText.textContent = String(charCount);
        enqueueChunk(event.data);
        return;
    }

    if (event.name === "summary") {
        summaryText.textContent = event.data || "暂无摘要";
        return;
    }

    if (event.name === "done") {
        setConversationId(event.data);
        setStatus("收尾中", "live");
        return;
    }

    if (event.name === "error") {
        throw new Error(event.data || "服务端返回 error 事件");
    }
}

function enqueueChunk(chunk) {
    if (!activeBubble) {
        return;
    }

    if (!smoothToggle.checked) {
        activeBubble.textContent += chunk;
        scrollMessages();
        return;
    }

    renderQueue.push(chunk);
    pumpRenderQueue();
}

function pumpRenderQueue() {
    if (renderTimer || !activeBubble) {
        return;
    }

    const renderNext = () => {
        if (!activeBubble || renderQueue.length === 0) {
            renderTimer = 0;
            return;
        }

        activeBubble.textContent += renderQueue.shift();
        scrollMessages();
        renderTimer = window.setTimeout(renderNext, 18);
    };

    renderTimer = window.setTimeout(renderNext, 0);
}

function waitForRenderQueue() {
    return new Promise((resolve) => {
        const check = () => {
            if (!renderTimer && renderQueue.length === 0) {
                resolve();
                return;
            }

            window.setTimeout(check, 20);
        };

        check();
    });
}

function appendMessage(role, content) {
    const item = document.createElement("article");
    item.className = `message ${role}`;

    const meta = document.createElement("div");
    meta.className = "message-meta";
    meta.textContent = role === "user" ? "你" : "助手";

    const bubble = document.createElement("div");
    bubble.className = "bubble";
    bubble.textContent = content;

    item.append(meta, bubble);
    messages.append(item);
    scrollMessages();

    return item;
}

function addEvent(name, data) {
    const item = document.createElement("article");
    item.className = "event-item";
    item.dataset.event = name;

    const heading = document.createElement("div");
    heading.className = "event-name";

    const eventName = document.createElement("span");
    eventName.textContent = name;

    const eventTime = document.createElement("span");
    eventTime.className = "event-time";
    eventTime.textContent = new Date().toLocaleTimeString();

    const body = document.createElement("div");
    body.className = "event-data";
    body.textContent = trimEventData(data);

    heading.append(eventName, eventTime);
    item.append(heading, body);
    events.prepend(item);
}

function trimEventData(data) {
    if (!data) {
        return "(empty)";
    }

    return data.length > 500 ? `${data.slice(0, 500)}...` : data;
}

function setConversationId(conversationId) {
    if (!conversationId) {
        return;
    }

    conversationInput.value = conversationId;
    localStorage.setItem(storageKey, conversationId);
}

function removeEmptyState() {
    const empty = messages.querySelector(".empty-state");

    if (empty) {
        empty.remove();
    }
}

function scrollMessages() {
    messages.scrollTop = messages.scrollHeight;
}

function updateElapsed() {
    if (!startedAt) {
        elapsedText.textContent = "0.0s";
        return;
    }

    elapsedText.textContent = `${((performance.now() - startedAt) / 1000).toFixed(1)}s`;
}

async function checkBackend() {
    try {
        setStatus("检查中", "live");
        const response = await fetch("/chat/test");
        const data = await response.json();
        addEvent("health", JSON.stringify(data, null, 2));
        await refreshConfig();
        setStatus(response.ok ? "后端正常" : "检查失败", response.ok ? "idle" : "error");
    } catch (error) {
        addEvent("error", error.message);
        setStatus("检查失败", "error");
    }
}

async function refreshConfig() {
    try {
        const response = await fetch("/chat/config");
        const result = await response.json();
        renderProviderNotice(result.data || {});
    } catch (error) {
        renderProviderNotice({
            provider: "unknown",
            model: "unknown",
            apiKeyConfigured: false,
            error: error.message
        });
    }
}

function renderProviderNotice(config) {
    const provider = String(config.provider || "unknown").toLowerCase();
    const model = config.model || "unknown";

    providerNotice.hidden = false;
    providerNotice.classList.toggle("warning", provider === "mock"
            || (provider === "deepseek" && !config.apiKeyConfigured)
            || provider === "unknown");

    if (provider === "mock") {
        providerNotice.textContent = `当前后端是 mock，返回内容是模拟模型，不是 DeepSeek。model=${model}`;
        return;
    }

    if (provider === "deepseek" && !config.apiKeyConfigured) {
        providerNotice.textContent = `当前配置为 DeepSeek，但 DEEPSEEK_API_KEY 未配置，真实调用会失败。model=${model}`;
        return;
    }

    if (provider === "deepseek") {
        providerNotice.textContent = `当前后端是 DeepSeek，正在调用真实模型。model=${model}`;
        return;
    }

    providerNotice.textContent = `当前后端 provider=${provider}，model=${model}`;
}

async function loadConversation() {
    const conversationId = conversationInput.value.trim();

    if (!conversationId) {
        setStatus("缺少会话ID", "error");
        return;
    }

    try {
        const response = await fetch(`/conversation/${encodeURIComponent(conversationId)}`);
        const result = await response.json();
        addEvent("conversation", JSON.stringify(result, null, 2));

        const data = result.data;
        if (data && Array.isArray(data.messages)) {
            removeEmptyState();
            messages.replaceChildren();
            data.messages.forEach((message) => appendMessage(message.role, message.content));
            summaryText.textContent = data.summary || "暂无摘要";
            setStatus("已读取", "idle");
        }
    } catch (error) {
        addEvent("error", error.message);
        setStatus("读取失败", "error");
    }
}

async function clearConversation() {
    const conversationId = conversationInput.value.trim();

    if (!conversationId) {
        startNewConversation();
        return;
    }

    try {
        const response = await fetch(`/conversation/${encodeURIComponent(conversationId)}`, {
            method: "DELETE"
        });
        const result = await response.json();
        addEvent("clear", JSON.stringify(result, null, 2));
        startNewConversation();
        setStatus(response.ok ? "已清空" : "清空失败", response.ok ? "idle" : "error");
    } catch (error) {
        addEvent("error", error.message);
        setStatus("清空失败", "error");
    }
}

function startNewConversation() {
    if (abortController) {
        abortController.abort();
    }

    localStorage.removeItem(storageKey);
    conversationInput.value = "";
    summaryText.textContent = "暂无摘要";
    resetStats();
    setStatus("新会话", "idle");
    messages.replaceChildren(createEmptyState());
}

function createEmptyState() {
    const empty = document.createElement("div");
    empty.className = "empty-state";

    const title = document.createElement("h3");
    title.textContent = "发一条消息，观察 SSE 事件如何逐步抵达。";

    const text = document.createElement("p");
    text.textContent = "页面会记录 start、message、summary、done 等事件。";

    empty.append(title, text);
    return empty;
}
