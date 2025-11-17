<script>
    import Theme from "./Theme.svelte";
    import { loggedIn, profile, sessionToken, pubkey, dashboardTab } from '../state.svelte.js';
    import { ws, messages } from '../socket.svelte.js';
import { onMount, onDestroy } from 'svelte';
    import Subscription from './Subscription.svelte';
const LEFT_WIDTH_KEY = 'dashboardLeftWidth';
const RIGHT_WIDTH_KEY = 'dashboardRightWidth';

const MIN_SIDE_WIDTH = 15;
const MAX_SIDE_WIDTH = 55;
const MIN_CENTER_WIDTH = 20;

let leftColumnWidth = 10;
let rightColumnWidth = 55; // percentage

let isDragging = false;
let dragTarget = null;
let hasMounted = false;
let containerElement;

onMount(() => {
    // console.log($profile);
    ws.send('RequestPaymentsHistory,' + $sessionToken);

    if (typeof window !== 'undefined') {
      const storedLeft = parseFloat(localStorage.getItem(LEFT_WIDTH_KEY));
      const storedRight = parseFloat(localStorage.getItem(RIGHT_WIDTH_KEY));

      if (!Number.isNaN(storedLeft)) {
        updateLeftWidth(storedLeft);
      }
      if (!Number.isNaN(storedRight)) {
        updateRightWidth(storedRight);
      }
    }

    hasMounted = true;

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  })

onDestroy(() => {
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
});

function testToken() {
    ws.send('RequestSinglePayment,' + $sessionToken);
}
function logout() {
    loggedIn.set(false);
    sessionToken.set(null);
    profile.set(null);
    pubkey.set(null);
}

let amount = "9.99";
let description = 'Test payment';
let paymentType = 'single';
let isSatsSelected = false;
let currency = 'EUR';
function sendPayment() {

  if (isSatsSelected) {
    ws.send('RequestSinglePayment,' + $sessionToken + ',Millisats,' + (amount + '000') + ',' + description + ',' + paymentType);
  } else {
    ws.send('RequestSinglePayment,' + $sessionToken + ',' + currency + ',' + amount + ',' + description + ',' + paymentType);
  }
}

$: if (!isSatsSelected) {
  if(!amount.includes('.')) {
    amount = amount + '.00';
  }
}

// remove . and 2 digits after the dot from amount if sats is selected
$: if (isSatsSelected) {

  if(amount.includes('.')) {
    amount = amount.slice(0, -3);
  }
}

const uiKit =
  typeof window !== 'undefined'
    ? /** @type {any} */ (Reflect.get(window, 'UIkit'))
    : undefined;

let paymentsHistory = [];

$: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'PaymentsHistory') {
      console.log('PaymentsHistory', lastMessage.history);
      paymentsHistory = lastMessage.history;
    }
    if (lastMessage.cmd === 'CashuSent') {

      uiKit?.notification('Minted token and sent to you!');
      
    }
    if (lastMessage.cmd === 'BurnToken') {
      uiKit?.notification('Burned ' + lastMessage.amount + ' tokens successfully!');
    }
  }

function formatPaymentStatus(payment) {
  if (payment.paid === undefined || payment.paid === null) {
    return 'Pending';
  }
  return payment.paid ? 'Paid' : 'Not Paid';
}

function formatPaymentStatusClass(payment) {
  if (payment.paid === undefined || payment.paid === null) {
    return '';
  }
  return payment.paid ? 'uk-badge-secondary' : 'uk-badge-destructive';
}


// cashu
let mintUrl = 'https://mint.getportal.cc';
let staticToken = 'test-static-token-for-mint-getportal-cc';

let currencyUnit = 'multi';

let cashuAmount= 1;
let cashuDescription = 'Test cashu token';
let cashuAmountToRequest = 1;

function mintAndSendToken() {
  ws.send('CashuMintAndSend,' + $sessionToken + ',' + mintUrl + ',' + staticToken + ',' + currencyUnit + ',' + cashuAmount + ',' + cashuDescription);
}

function burnToken() {
  ws.send('BurnToken,' + $sessionToken + ',' + mintUrl + ',' + staticToken + ',' + currencyUnit + ',' + cashuAmountToRequest);
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function updateLeftWidth(value) {
  const upperBound = Math.max(
    0,
    Math.min(MAX_SIDE_WIDTH, 100 - MIN_CENTER_WIDTH - rightColumnWidth)
  );
  leftColumnWidth = clamp(value, 0, upperBound);
}

function updateRightWidth(value) {
  const upperBound = Math.max(
    MIN_SIDE_WIDTH,
    Math.min(MAX_SIDE_WIDTH, 100 - MIN_CENTER_WIDTH - leftColumnWidth)
  );
  rightColumnWidth = clamp(value, MIN_SIDE_WIDTH, upperBound);
}

function handleMouseDown(target, e) {
  dragTarget = target;
  isDragging = true;
  e.preventDefault();
  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
}

function handleMouseMove(e) {
  if (!isDragging || !containerElement || !dragTarget) return;
  
  const rect = containerElement.getBoundingClientRect();
  const containerWidth = rect.width;
  const mouseX = e.clientX - rect.left;

  if (dragTarget === 'left') {
    const newWidth = (mouseX / containerWidth) * 100;
    updateLeftWidth(newWidth);
  } else if (dragTarget === 'right') {
    const newWidth = ((containerWidth - mouseX) / containerWidth) * 100;
    updateRightWidth(newWidth);
  }
}

function handleMouseUp() {
  isDragging = false;
  dragTarget = null;
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
}

$: if (hasMounted && typeof window !== 'undefined') {
  localStorage.setItem(LEFT_WIDTH_KEY, String(leftColumnWidth));
  localStorage.setItem(RIGHT_WIDTH_KEY, String(rightColumnWidth));
}

</script>

          <div class="p-3 lg:p-5">
            <div class="space-y-0.5">
              <h2 class="text-2xl font-bold tracking-tight">Dashboard</h2>
              <p class="text-muted-foreground">
                Welcome, {$profile.name}!
              </p>
            </div>
            <div class="border-border my-6 border-t"></div>
            <div class="flex" bind:this={containerElement}>
              <aside class="mr-6" style="width: {leftColumnWidth}%; max-width: 45%;">
                <ul
                  class="uk-nav uk-nav-secondary"
                  data-uk-switcher="connect: #component-nav; animation: uk-anmt-slide-left-sm"
                >
                  
                  <li class:uk-active={$dashboardTab === 'payment'}><a href="/" on:click|preventDefault={() => dashboardTab.set('payment')}>Payment</a></li>
                  <li class:uk-active={$dashboardTab === 'cashu'}><a href="/" on:click|preventDefault={() => dashboardTab.set('cashu')}>Cashu</a></li>
                  <li class:uk-active={$dashboardTab === 'subscriptions'}><a href="/" on:click|preventDefault={() => dashboardTab.set('subscriptions')}>Subscriptions</a></li>
                  <li class:uk-active={$dashboardTab === 'profile'}><a href="/" on:click|preventDefault={() => dashboardTab.set('profile')}>Profile</a></li>                  
                  <!-- <li><a href="#">Account</a></li> -->
                  <li class:uk-active={$dashboardTab === 'appearance'}><a href="/" on:click|preventDefault={() => dashboardTab.set('appearance')}>Appearance</a></li>
                  <!-- <li><a href="#">Notifications</a></li>
                  <li><a href="#">Display</a></li> -->

                </ul>
              </aside>
              <button 
                type="button"
                class="resizer"
                class:dragging={isDragging && dragTarget === 'left'}
                on:mousedown={(e) => handleMouseDown('left', e)}
                aria-label="Resize navigation panel"
              ></button>
              <div class="flex-1 mr-6 ml-6" style="min-width: 0;">
                <ul id="component-nav" class="uk-switcher max-w-2xl">
                    <li class="uk-active space-y-6">
                        <div>
                          <h3 class="text-lg font-medium">Payment</h3>
                          <p class="text-muted-foreground text-sm">
                            Request a single payment.
                          </p>
                        </div>
                        <div class="border-border border-t"></div>
                     
                        <div class="space-y-2">
                          <label class="uk-form-label" for="username">Currency</label>
                          <div class="flex items-center space-x-2">
                            <input
                              class="uk-toggle-switch uk-toggle-switch-primary"
                              id="toggle-switch"
                              type="checkbox"
                              bind:checked={isSatsSelected}
                            />
                            <label class="uk-form-label" for="toggle-switch">Sats</label>
                          </div>
                          {#if !isSatsSelected}
                            <input class="uk-input w-24" type="text" placeholder="EUR" bind:value={currency} />
                          {/if}
                          <div class="uk-form-help text-muted-foreground">
                            Select the currency you want to pay in.
                          </div>
                        </div>

                        <div class="space-y-2">
                          <label class="uk-form-label" for="username">Amount</label>
                          <input
                            class="uk-input"
                            id="amount"
                            type="text"
                            placeholder="100"
                            bind:value={amount}
                          />
                          <div class="uk-form-help text-muted-foreground">
                            Enter the amount you want to pay.
                          </div>
                        </div>
                        <div class="space-y-2">
                          <label class="uk-form-label" for="description"
                            >Description</label
                          >
                          <textarea
                            class="uk-textarea"
                            id="description"
                            placeholder="Test payment"
                            bind:value={description}
                          ></textarea>
                          <div class="uk-form-help text-muted-foreground">
                            Enter a description for the payment.
                          </div>
                        </div>
                        <!-- <div class="space-y-2">
                            <label class="uk-form-label block" for="email">Payment Type</label>
                            <div class="h-10">
                              <uk-select
                                icon="chevrons-up-down"
                                class="fr-select"
                                cls-custom="button: uk-input-fake justify-between; dropdown: w-full"
                                name="paymentType"
                                id="paymentType"
                              >
                                <select hidden>
                                  <optgroup label="Select a payment type">
                                    <option selected>Single</option>
                                    <option disabled>Recurring</option>
                                  </optgroup>
                                </select>
                              </uk-select>
                            </div>
                            <div class="uk-form-help text-muted-foreground">
                              Select the type of payment you want to make.
                            </div>
                        </div> -->
                        <div class="">
                          <button class="uk-btn uk-btn-primary" on:click={sendPayment}>Send Payment Request</button>
                        </div>
                        
                    </li>
                    <li class="uk-active space-y-6">
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
                        <label class="uk-form-label" for="description"
                          >Description</label
                        >
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
                        <label class="uk-form-label" for="description"
                          >Currency Unit</label
                        >
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
                        <label class="uk-form-label" for="description"
                          >Static Token</label
                        >
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
   
                  </li>
                  <li class="uk-active space-y-6">
                    <Subscription />
                  </li>
                  <li class="space-y-6">
                    <div>
                      <h3 class="text-lg font-medium">Profile</h3>
                      <p class="text-muted-foreground text-sm">
                        This is how others will see you on the site.
                      </p>
                    </div>
                    <div class="border-border border-t"></div>
                    <div class="space-y-2">
                      <label class="uk-form-label" for="pubkey">Public Key</label>
                      <input
                        class="uk-input"
                        id="pubkey"
                        type="text"
                        value="{$pubkey}"
                        disabled
                      />
                      <div class="uk-form-help text-muted-foreground">
                        This is your public key. It is used to identify you on the network.
                      </div>
                    </div>         
                    <div class="space-y-2">
                      <label class="uk-form-label" for="name">Name</label>
                      <input
                        class="uk-input"
                        id="name"
                        type="text"
                        value="{$profile.name}"
                        disabled
                      />
                      <div class="uk-form-help text-muted-foreground">
                        This is your public display name. It can be your real name or a
                        pseudonym.
                      </div>
                    </div>
                    <div class="space-y-2">
                      <label class="uk-form-label" for="email">Display Name</label>
                      <input
                        class="uk-input"
                        id="displayName"
                        type="text"
                        value="{$profile.displayName}"
                        disabled
                      />
                      <div class="uk-form-help text-muted-foreground">
                        This is your display name.
                      </div>
                    </div>
                    <div class="space-y-2">
                      <label class="uk-form-label" for="nip05"
                        >Nip05</label
                      >
                      <input
                        class="uk-input"
                        id="nip05"
                        type="text"
                        value="{$profile.nip05}"
                        disabled
                      />
                      <div class="uk-form-help text-muted-foreground">
                        This is your Nip05 address.
                      </div>
                    </div>
                    <div class="">
                      <button class="uk-btn uk-btn-primary" on:click={logout}>Logout</button>
                    </div>
                  </li>
                  <!-- <li class="space-y-6">
                    <div>
                      <h3 class="text-lg font-medium">Account</h3>
                      <p class="text-muted-foreground text-sm">
                        Update your account settings. Set your preferred language and
                        timezone.
                      </p>
                    </div>
                    <div class="border-border border-t"></div>
                    <div class="space-y-2">
                      <label class="uk-form-label" for="name">Name</label>
                      <input
                        class="uk-input"
                        id="name"
                        type="text"
                        placeholder="Your name"
                      />
                      <div class="uk-form-help text-muted-foreground">
                        This is the name that will be displayed on your profile and in
                        emails.
                      </div>
                    </div>
                    <div class="space-y-2">
                      <label class="uk-form-label block" for="date_of_birth"
                        >Date of Birth</label
                      >
                      <div class="h-10 w-[278px]">
                        <uk-input-date
                          jumpable="true"
                          icon="calendar"
                          cls-custom="uk-input-fake justify-between"
                          placeholder="Pick a date"
                        ></uk-input-date>
                      </div>
                      <div class="uk-form-help text-muted-foreground">
                        Your date of birth is used to calculate your age.
                      </div>
                    </div>
                    <div class="space-y-2">
                      <label class="uk-form-label block" for="language">Language</label>
                      <div class="h-10">
                        <uk-select
                          icon="chevrons-up-down"
                          class="fr-select"
                          cls-custom="button: uk-input-fake justify-between; dropdown: w-full"
                          name="language"
                          placeholder="Select a language"
                          searchable="true"
                        >
                          <select hidden>
                            <optgroup label="Select a language">
                              <option selected>English</option>
                              <option>French</option>
                              <option>German</option>
                              <option>Spanish</option>
                              <option>Portuguese</option>
                            </optgroup>
                          </select>
                        </uk-select>
                      </div>
                      <div class="uk-form-help text-muted-foreground">
                        This is the language that will be used in the dashboard.
                      </div>
                    </div>
                    <div class="">
                      <button class="uk-btn uk-btn-primary">Update profile</button>
                    </div>
                  </li> -->
                  <li class="space-y-6">
                    <div>
                      <h3 class="text-lg font-medium">Appearance</h3>
                      <p class="text-muted-foreground text-sm">
                        Customize the appearance of the app. Automatically switch
                        between day and night themes.
                      </p>
                    </div>
                    <div class="border-border border-t"></div>
                    <div class="space-y-2">
                      <label class="uk-form-label block" for="email">Font Family</label>
                      <div class="h-10">
                        <uk-select
                          icon="chevrons-up-down"
                          class="fr-select"
                          cls-custom="button: uk-input-fake justify-between; dropdown: w-full"
                          name="email"
                          id="email"
                        >
                          <select hidden>
                            <optgroup label="Select a font family">
                              <option>Inter</option>
                              <option selected>Geist</option>
                              <option>Open Sans</option>
                            </optgroup>
                          </select>
                        </uk-select>
                      </div>
                      <div class="uk-form-help text-muted-foreground">
                        Set the font you want to use in the dashboard.
                      </div>
                    </div>
                    <div class="space-y-2">
                      <span class="uk-form-label">Theme</span>
                      <div class="uk-form-help text-muted-foreground">
                        Select the theme for the dashboard.
                      </div>
                      <div class="grid max-w-md grid-cols-2 gap-8">
                        <uk-lsh cls-custom="w-full" value="light" group="mode">
                          <template>
                            <div
                              class="border-muted ring-ring block cursor-pointer items-center rounded-md border-2 p-1"
                            >
                              <div class="space-y-2 rounded-sm bg-[#ecedef] p-2">
                                <div
                                  class="space-y-2 rounded-md bg-white p-2 shadow-sm"
                                >
                                  <div
                                    class="h-2 w-[80px] rounded-lg bg-[#ecedef]"
                                  ></div>
                                  <div
                                    class="h-2 w-[100px] rounded-lg bg-[#ecedef]"
                                  ></div>
                                </div>
                                <div
                                  class="flex items-center space-x-2 rounded-md bg-white p-2 shadow-sm"
                                >
                                  <div class="h-4 w-4 rounded-full bg-[#ecedef]"></div>
                                  <div
                                    class="h-2 w-[100px] rounded-lg bg-[#ecedef]"
                                  ></div>
                                </div>
                                <div
                                  class="flex items-center space-x-2 rounded-md bg-white p-2 shadow-sm"
                                >
                                  <div class="h-4 w-4 rounded-full bg-[#ecedef]"></div>
                                  <div
                                    class="h-2 w-[100px] rounded-lg bg-[#ecedef]"
                                  ></div>
                                </div>
                              </div>
                            </div>
                          </template>
                        </uk-lsh>
                        <uk-lsh cls-custom="w-full" value="dark" group="mode">
                          <template>
                            <div
                              class="border-muted bg-popover ring-ring block cursor-pointer items-center rounded-md border-2 p-1"
                            >
                              <div class="space-y-2 rounded-sm bg-slate-950 p-2">
                                <div
                                  class="space-y-2 rounded-md bg-slate-800 p-2 shadow-sm"
                                >
                                  <div
                                    class="h-2 w-[80px] rounded-lg bg-slate-400"
                                  ></div>
                                  <div
                                    class="h-2 w-[100px] rounded-lg bg-slate-400"
                                  ></div>
                                </div>
                                <div
                                  class="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm"
                                >
                                  <div class="h-4 w-4 rounded-full bg-slate-400"></div>
                                  <div
                                    class="h-2 w-[100px] rounded-lg bg-slate-400"
                                  ></div>
                                </div>
                                <div
                                  class="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm"
                                >
                                  <div class="h-4 w-4 rounded-full bg-slate-400"></div>
                                  <div
                                    class="h-2 w-[100px] rounded-lg bg-slate-400"
                                  ></div>
                                </div>
                              </div>
                            </div>
                          </template>
                        </uk-lsh>
                      </div>
                    </div>
                    <div class="">
                      <button class="uk-btn uk-btn-primary">Update preferences</button>
                    </div>
                  </li>
                  <!-- <li class="space-y-6">
                    <div>
                      <h3 class="text-lg font-medium">Notifications</h3>
                      <p class="text-muted-foreground text-sm">
                        Configure how you receive notifications.
                      </p>
                    </div>
                    <div class="border-border border-t"></div>
                    <div class="space-y-2">
                      <span class="uk-form-label"> Notify me about </span>
                      <label class="block text-sm" for="notification_0">
                        <input
                          id="notification_0"
                          class="uk-radio mr-2"
                          name="notification"
                          type="radio"
                        />
                        All new messages
                      </label>
                      <label class="block text-sm" for="notification_1">
                        <input
                          id="notification_1"
                          class="uk-radio mr-2"
                          name="notification"
                          type="radio"
                        />
                        Direct messages and mentions
                      </label>
                      <label class="block text-sm" for="notification_2">
                        <input
                          id="notification_2"
                          class="uk-radio mr-2"
                          name="notification"
                          type="radio"
                          checked
                        />
                        Nothing
                      </label>
                    </div>
                    <div>
                      <h3 class="mb-4 text-lg font-medium">Email Notifications</h3>
                      <div class="space-y-4">
                        <div
                          class="border-border flex items-center justify-between rounded-lg border p-4"
                        >
                          <div class="space-y-0.5">
                            <label
                              class="text-base font-medium"
                              for="email_notification_0"
                            >
                              Communication emails
                            </label>
                            <div class="uk-form-help text-muted-foreground">
                              Receive emails about your account activity.
                            </div>
                          </div>
                          <input
                            class="uk-toggle-switch uk-toggle-switch-primary"
                            id="email_notification_0"
                            type="checkbox"
                          />
                        </div>
                        <div
                          class="border-border flex items-center justify-between rounded-lg border p-4"
                        >
                          <div class="space-y-0.5">
                            <label
                              class="text-base font-medium"
                              for="email_notification_1"
                            >
                              Marketing emails
                            </label>
                            <div class="uk-form-help text-muted-foreground">
                              Receive emails about new products, features, and more.
                            </div>
                          </div>
                          <input
                            class="uk-toggle-switch uk-toggle-switch-primary"
                            id="email_notification_1"
                            type="checkbox"
                          />
                        </div>
                        <div
                          class="border-border flex items-center justify-between rounded-lg border p-4"
                        >
                          <div class="space-y-0.5">
                            <label
                              class="text-base font-medium"
                              for="email_notification_2"
                            >
                              Social emails
                            </label>
                            <div class="uk-form-help text-muted-foreground">
                              Receive emails for friend requests, follows, and more.
                            </div>
                          </div>
                          <input
                            class="uk-toggle-switch uk-toggle-switch-primary"
                            id="email_notification_2"
                            type="checkbox"
                            checked
                          />
                        </div>
                        <div
                          class="border-border flex items-center justify-between rounded-lg border p-4"
                        >
                          <div class="space-y-0.5">
                            <label
                              class="text-base font-medium"
                              for="email_notification_3"
                            >
                              Security emails
                            </label>
                            <div class="uk-form-help text-muted-foreground">
                              Receive emails about your account activity and security.
                            </div>
                          </div>
                          <input
                            class="uk-toggle-switch uk-toggle-switch-primary"
                            id="email_notification_3"
                            type="checkbox"
                            checked
                            disabled
                          />
                        </div>
                      </div>
                    </div>
                    <div class="flex gap-x-3">
                      <input
                        class="uk-checkbox mt-1"
                        id="notification_mobile"
                        type="checkbox"
                        checked
                      />
                      <div class="space-y-1">
                        <label class="uk-form-label" for="notification_mobile">
                          Use different settings for my mobile devices
                        </label>
                        <div class="uk-form-help text-muted-foreground">
                          You can manage your mobile notifications in the mobile
                          settings page.
                        </div>
                      </div>
                    </div>
                    <div class="">
                      <button class="uk-btn uk-btn-primary">
                        Update notifications
                      </button>
                    </div>
                  </li> -->
                  <!-- <li class="space-y-6">
                    <div>
                      <h3 class="text-lg font-medium">Display</h3>
                      <p class="text-muted-foreground text-sm">
                        Turn items on or off to control what's displayed in the app.
                      </p>
                    </div>
                    <div class="border-border border-t"></div>
                    <div class="space-y-2">
                      <div class="mb-4">
                        <span class="text-base font-medium"> Sidebar </span>
                        <div class="uk-form-help text-muted-foreground">
                          Select the items you want to display in the sidebar.
                        </div>
                      </div>
                      <label class="block text-sm" for="display_0">
                        <input class="uk-checkbox mr-2" type="checkbox" checked />
                        Recents
                      </label> -->

                </ul>
              </div>
              <button 
                type="button"
                class="resizer"
                class:dragging={isDragging && dragTarget === 'right'}
                on:mousedown={(e) => handleMouseDown('right', e)}
                aria-label="Resize payment history panel"
              ></button>
              <aside class="ml-6" style="width: {rightColumnWidth}%; min-width: 200px; max-width: 50%;">
                <div class="space-y-6">
                  <div>
                    <h3 class="text-lg font-medium">Payment History</h3>
                    <p class="text-muted-foreground text-sm">
                      This is a list of your payment history.
                    </p>
                  </div>
                  <div class="border-border border-t"></div>
                  <div class="overflow-x-auto">
                    <table class="uk-table uk-table-divider uk-table-small">
                      <thead>
                        <tr>
                          <th>Currency</th>
                          <th>Amount</th>
                          <th>Description</th>
                          <th>Status</th>
                          <th>Created At</th>
                        </tr>
                      </thead>
                      <tbody>
                        {#each paymentsHistory as payment}
                          <tr>
                            <td>{payment.currency === 'Millisats' ? 'Sats' : payment.currency}</td>
                            <td>{payment.currency === 'Millisats' ? payment.amount / 1000 : payment.amount / 100}</td>
                            <td>{payment.description}</td>
                            <td><span class="uk-badge {formatPaymentStatusClass(payment)}">{formatPaymentStatus(payment)}</span></td>
                            <td>{payment.createdAt}</td>
                          </tr>
                        {/each}
                      </tbody>
                    </table>
                  </div>
                </div>
              </aside>
            </div>
          </div>



<style> 
:global(.fr-select li.uk-active > a) {
  --uk-nav-item-bg: hsl(var(--accent));
  --uk-nav-item-color: hsl(var(--accent-foreground));
}

.resizer {
  width: 2px;
  background-color: hsl(var(--border) / 0.35);
  cursor: col-resize;
  user-select: none;
  transition: background-color 0.2s;
  flex-shrink: 0;
  border: none;
  padding: 0;
  align-self: stretch;
  background-clip: padding-box;
}

.resizer:hover,
.resizer:focus-visible {
  background-color: hsl(var(--accent) / 0.6);
  outline: none;
}

.resizer.dragging {
  background-color: hsl(var(--accent));
}
</style>