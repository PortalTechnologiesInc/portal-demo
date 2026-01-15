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
    <h3 class="text-lg font-medium">Cashu Token Demo</h3>
    <p class="text-muted-foreground text-sm">
      Mint and burn your tokens.
    </p>
  </div>
  <div class="border-border border-t"></div>
  <div class="space-y-2">
    <label class="uk-form-label" for="username">Amount</label>
    <input
      class="uk-input"
      id="amount"
      type="text"
      placeholder="1"
      bind:value={cashuAmount}
    />
    <div class="uk-form-help text-muted-foreground">
      Enter the amount you want to mint.
    </div>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="description">Description</label>
    <textarea
      class="uk-textarea"
      id="description"
      placeholder="Test token"
      bind:value={cashuDescription}
    ></textarea>
    <div class="uk-form-help text-muted-foreground">
      Enter a description for the token.
    </div>
  </div>
  <div class="">
    <button class="uk-btn uk-btn-primary" on:click={mintAndSendToken}>Mint & Send Token</button>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="username">Amount to Request</label>
    <input
      class="uk-input"
      id="amountToRequest"
      type="text"
      placeholder="1"
      bind:value={cashuAmountToRequest}
    />
    <div class="uk-form-help text-muted-foreground">
      Enter the amount you want to request.
    </div>
  </div>
  <div class="">
    <button class="uk-btn uk-btn-primary" on:click={burnToken}>Burn Token</button>
  </div>

  <hr class="uk-divider-icon" />

  <div>
    <h3 class="text-lg font-medium">Cashu Token Settings</h3>
    <p class="text-muted-foreground text-sm">
      Manage your tickets and redeem them.
    </p>
  </div>
  <div class="border-border border-t"></div>
 
  <div class="space-y-2">
    <label class="uk-form-label" for="username">Mint URL</label>
    <input
      class="uk-input"
      id="mintUrl"
      type="text"
      placeholder="https://cashu.example.com"
      bind:value={mintUrl}
    />
    <div class="uk-form-help text-muted-foreground">
      Enter the URL of the mint you want to use.
    </div>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="description">Currency Unit</label>
    <input
      class="uk-input"
      id="currencyUnit"
      type="text"
      placeholder="multi"
      bind:value={currencyUnit}
    />
    <div class="uk-form-help text-muted-foreground">
      Enter the unit of the currency you want to use.
    </div>
  </div>
  <div class="space-y-2">
    <label class="uk-form-label" for="description">Static Token</label>
    <input
      class="uk-input"
      id="staticToken"
      type="text"
      placeholder="test-static-token-for-mint-getportal-cc"
      bind:value={staticToken}
    />
    <div class="uk-form-help text-muted-foreground">
      Enter the static token you want to use.
    </div>
  </div>                      
</div>
