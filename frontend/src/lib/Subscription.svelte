<script>
import { ws, messages } from '../socket.svelte.js';
import { sessionToken } from '../state.svelte.js';
import { onMount } from 'svelte';
onMount(() => {
  // every 60 seconds request the subscriptions history
  ws.send('RequestSubscriptionsHistory,' + $sessionToken);
  setInterval(() => {
    ws.send('RequestSubscriptionsHistory,' + $sessionToken);
  }, 60000);
})

let amount = "9.99";
let description = 'Test payment';
let frequency = 'Minutely';
let isSatsSelected = false;
let currency = 'EUR';
function sendPayment() {

  console.log('frequency', frequency);
  if (isSatsSelected) {
    ws.send('RequestRecurringPayment,' + $sessionToken + ',Millisats,' + (amount + '000') + ',' + description + ',' + frequency.toLowerCase());
  } else {
    ws.send('RequestRecurringPayment,' + $sessionToken + ',' + currency + ',' + amount + ',' + description + ',' + frequency.toLowerCase());
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

let subscriptionsHistory = [];

$: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'SubscriptionsHistory') {
      console.log('SubscriptionsHistory', lastMessage.history);
      subscriptionsHistory = lastMessage.history;
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


</script>

                        <div>
                          <h3 class="text-lg font-medium">Subscription</h3>
                          <p class="text-muted-foreground text-sm">
                            Request a recurring payment.
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
                        <div class="space-y-2">
                            <label class="uk-form-label block" for="email">Frequency</label>
                            <div class="h-10">
                              <uk-select
                                icon="chevrons-up-down"
                                class="fr-select"
                                cls-custom="button: uk-input-fake justify-between; dropdown: w-full"
                                name="frequency"
                                id="paymentType"
                              >
                                <select hidden>
                                  <optgroup label="Select a payment type">
                                    <option selected>Minutely</option>
                                    <option>Hourly</option>
                                    <option>Daily</option>
                                    <option>Weekly</option>
                                    <option>Monthly</option>
                                    <option>Yearly</option>
                                  </optgroup>
                                </select>
                              </uk-select>
                            </div>
                            <div class="uk-form-help text-muted-foreground">
                              Select the frequency of the payment.
                            </div>
                        </div>
                        <div class="">
                          <button class="uk-btn uk-btn-primary" on:click={sendPayment}>Send Payment Request</button>
                        </div>

                        <hr class="uk-divider-icon" />
                        
                        <div>
                          <h3 class="text-lg font-medium">Subscriptions</h3>
                          <p class="text-muted-foreground text-sm">
                            This is a list of your subscriptions.
                          </p>
                        </div>
                        <div class="border-border border-t"></div>
                        <table class="uk-table uk-table-divider">
                          <thead>
                            <tr>
                              <th>Currency</th>
                              <th>Amount</th>
                              <th>Frequency</th>
                              <th>Description</th>
                              <th>Status</th>
                              <th>Created At</th>
                            </tr>
                          </thead>
                          <tbody>
                            {#each subscriptionsHistory as subscription}
                              <tr>
                                <td>{subscription.currency === 'Millisats' ? 'Sats' : subscription.currency}</td>
                                <td>{subscription.currency === 'Millisats' ? subscription.amount / 1000 : subscription.amount / 100}</td>
                                <td>{subscription.frequency.toLowerCase()}</td>
                                <td>{subscription.description}</td>
                                <td><span class="uk-badge">{subscription.status.toLowerCase()}</span></td>
                                <td>{subscription.createdAt}</td>
                              </tr>
                            {/each}
                          </tbody>
                        </table>
                        
