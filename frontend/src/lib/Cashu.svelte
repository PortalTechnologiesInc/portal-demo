<script>
  import { sendWsMessage, messages } from '../socket.svelte.js';
  import { sessionToken } from '../state.svelte.js';

  const uiKit =
    typeof window !== 'undefined'
      ? /** @type {any} */ (Reflect.get(window, 'UIkit'))
      : undefined;

  let mintUrl = 'https://mint.getportal.cc';
  let staticToken = 'test-static-token-for-mint-getportal-cc';
  let currencyUnit = 'multi';
  let cashuAmount = 1;
  let cashuDescription = 'Test cashu token';
  let cashuAmountToRequest = 1;

  function mintAndSendToken() {
    sendWsMessage('CashuMintAndSend,' + $sessionToken + ',' + mintUrl + ',' + staticToken + ',' + currencyUnit + ',' + cashuAmount + ',' + cashuDescription);
  }

  function burnToken() {
    sendWsMessage('BurnToken,' + $sessionToken + ',' + mintUrl + ',' + staticToken + ',' + currencyUnit + ',' + cashuAmountToRequest);
  }

  $: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'CashuSent') {
      uiKit?.notification('Minted token and sent to you!');
    }
    if (lastMessage.cmd === 'BurnToken') {
      uiKit?.notification('Burned ' + lastMessage.amount + ' tokens successfully!');
    }
  }
</script>

<div class="space-y-6">
  <div>
    <h3 class="text-lg font-medium">Cashu</h3>
    <p class="text-muted-foreground text-sm">
      Mint and send your tokens.
    </p>
  </div>
  <div class="border-border border-t"></div>

  <!-- Shared Settings -->
  <div class="space-y-6">
    <div class="space-y-2">
      <label class="uk-form-label" for="mint-url">Mint URL</label>
      <input
        class="uk-input"
        id="mint-url"
        type="text"
        placeholder="https://cashu.example.com"
        bind:value={mintUrl}
      />
      <div class="uk-form-help text-muted-foreground">
        Enter the URL of the mint you want to use.
      </div>
    </div>
    <div class="space-y-2">
      <label class="uk-form-label" for="currency-unit">Currency Unit</label>
      <input
        class="uk-input"
        id="currency-unit"
        type="text"
        placeholder="multi"
        bind:value={currencyUnit}
      />
      <div class="uk-form-help text-muted-foreground">
        Enter the unit of the currency you want to use.
      </div>
    </div>
    <div class="space-y-2">
      <label class="uk-form-label" for="static-token">Static Token</label>
      <input
        class="uk-input"
        id="static-token"
        type="text"
        placeholder="test-static-token-for-mint-getportal-cc"
        bind:value={staticToken}
      />
      <div class="uk-form-help text-muted-foreground">
        Enter the static token you want to use.
      </div>
    </div>
  </div>

  <div class="border-border border-t"></div>

  <!-- Tab Navigation -->
  <ul
    class="uk-nav uk-nav-default uk-nav-tab cashu-tabs"
    data-uk-switcher="connect: #cashu-tabs; animation: uk-animation-fade"
  >
    <li class="uk-active"><a href="/" on:click|preventDefault>Mint</a></li>
    <li><a href="/" on:click|preventDefault>Burn</a></li>
  </ul>

  <!-- Tab Content -->
  <ul id="cashu-tabs" class="uk-switcher">
    <!-- Mint Tab -->
    <li class="uk-active">
      <div class="space-y-6">
        <div class="space-y-2">
          <label class="uk-form-label" for="mint-amount">Amount</label>
          <input
            class="uk-input"
            id="mint-amount"
            type="text"
            placeholder="1"
            bind:value={cashuAmount}
          />
          <div class="uk-form-help text-muted-foreground">
            Enter the amount you want to mint.
          </div>
        </div>
        <div class="space-y-2">
          <label class="uk-form-label" for="mint-description">Description</label>
          <textarea
            class="uk-textarea"
            id="mint-description"
            placeholder="Test token"
            bind:value={cashuDescription}
          ></textarea>
          <div class="uk-form-help text-muted-foreground">
            Enter a description for the token.
          </div>
        </div>
        <div>
          <button class="uk-btn uk-btn-primary" on:click={mintAndSendToken}>Mint & Send Token</button>
        </div>
      </div>
    </li>

    <!-- Burn Tab -->
    <li>
      <div class="space-y-6">
        <div class="space-y-2">
          <label class="uk-form-label" for="send-amount">Amount to Request</label>
          <input
            class="uk-input"
            id="send-amount"
            type="text"
            placeholder="1"
            bind:value={cashuAmountToRequest}
          />
          <div class="uk-form-help text-muted-foreground">
            Enter the amount you want to request.
          </div>
        </div>
        <div>
          <button class="uk-btn uk-btn-primary" on:click={burnToken}>Burn Token</button>
        </div>
      </div>
    </li>
  </ul>
</div>

<style>
  :global(.cashu-tabs) {
    display: flex;
    flex-direction: row;
  }
  
  :global(.cashu-tabs li) {
    display: inline-block;
  }
</style>
